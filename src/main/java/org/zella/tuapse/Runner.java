package org.zella.tuapse;

import io.reactivex.BackpressureOverflowStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.ipfs.impl.IpfsDisabled;
import org.zella.tuapse.model.messages.TypedMessage;
import org.zella.tuapse.model.messages.impl.SearchAnswer;
import org.zella.tuapse.server.TuapseServer;
import org.zella.tuapse.storage.Index;
import org.zella.tuapse.storage.impl.EsIndex;
import org.zella.tuapse.storage.impl.LuceneIndex;
import org.zella.tuapse.subprocess.Subprocess;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Runner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    private static final int WebtorrConcurency = Integer.parseInt(System.getenv().getOrDefault("WEBTORRENT_CONCURRENCY", "3"));
    private static final String IndexType = (System.getenv().getOrDefault("INDEX_TYPE", "EMBEDDED"));

    public static void main(String[] args) {

        final Index es;
        switch (IndexType) {
            case "EMBEDDED":
                var path = System.getenv("EMBEDDED_INDEX_DIR");
                Objects.requireNonNull(path);
                es = new LuceneIndex(Paths.get(path));
                break;
            case "ELASTICSEARCH":
                es = new EsIndex();
                break;
            default:
                es = null;
                System.err.println("Wrong n type");
                System.exit(-1);
        }

        try {
            es.createIndexIfNotExist();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }


        //TODO disable env?
        //TODO restart spider if no torrent more that 5-10 min
        Subprocess.spider()
                .retry()
                //TODO we should have large buffer and stop spider if buffer full until its free
                //bt idealy need investiogate bep and spider and reduce speed
                .onBackpressureBuffer(64, () -> logger.warn("Post process too slow!"),
                        BackpressureOverflowStrategy.DROP_LATEST)
                .flatMap(hash -> Subprocess.webtorrent(hash)
                                .flatMap(t -> Single.fromCallable(() -> es.insertTorrent(t)))
                                .toFlowable()
                                .doOnError(throwable -> logger.warn(throwable.getMessage()))
                                .onErrorResumeNext(Flowable.empty())
                        , WebtorrConcurency)
                .subscribeOn(Schedulers.io())
                .takeWhile(s -> es.isSpaceAllowed())
                .subscribe(s -> logger.info("Inserted: " + s));

        var server = new TuapseServer(es);
        server.listen()
                .subscribe();

        Single.fromCallable(Subprocess::ipfsRoom).flatMapCompletable(ipfs -> {
            server.ipfsUpdate(ipfs);

            Completable waitExit = ipfs.waitExit().flatMapCompletable(e -> Completable.error(new Exception("Process dead")))
                    .subscribeOn(Schedulers.io());

            Completable p2pSearches = ipfs.distributedSearches()
                    .subscribeOn(Schedulers.io())
                    .onBackpressureBuffer(4, () -> logger.warn("Search to slow!"),
                            BackpressureOverflowStrategy.DROP_LATEST)
                    .flatMapSingle(req -> Single.fromCallable(() -> es.search(req.m.searchString))
                            .flatMap(searchResult -> ipfs.searchAnswer(new TypedMessage<>(req.peerId, SearchAnswer.create(searchResult)))
                                    //TODO fix me, use completable
                                    .toSingleDefault("ok"))).ignoreElements();
            return Completable.merge(List.of(waitExit, p2pSearches));
        })
                //maybe another?
                .doOnError(e -> server.ipfsUpdate(new IpfsDisabled()))
                .retryWhen(throwables -> throwables.delay(5, TimeUnit.SECONDS))
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe();

    }
}
