package org.zella.tuapse;

import io.reactivex.*;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.importer.Importer;
import org.zella.tuapse.ipfs.impl.IpfsDisabled;
import org.zella.tuapse.model.messages.TypedMessage;
import org.zella.tuapse.model.messages.impl.SearchAnswer;
import org.zella.tuapse.providers.TuapseSchedulers;
import org.zella.tuapse.providers.Utils;
import org.zella.tuapse.search.Search;
import org.zella.tuapse.server.TuapseServer;
import org.zella.tuapse.storage.Index;
import org.zella.tuapse.storage.impl.EsIndex;
import org.zella.tuapse.storage.impl.LuceneIndex;
import org.zella.tuapse.storage.impl.fake.MockEsSearch;
import org.zella.tuapse.subprocess.Subprocess;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Runner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    private static final String IndexType = (System.getenv().getOrDefault("INDEX_TYPE", "EMBEDDED"));

    public static void main(String[] args) {


        final Index index;
        switch (IndexType) {
            case "EMBEDDED":
                var path = System.getenv("EMBEDDED_INDEX_DIR");
                if (path == null) {
                    System.err.println("EMBEDDED_INDEX_DIR env variable not set");
                    System.exit(-1);
                }
                index = new LuceneIndex(Paths.get(path));
                break;
            case "ELASTICSEARCH":
                index = new EsIndex();
                break;
            case "MOCK":
                index = new MockEsSearch();
                break;
            default:
                index = null;
                System.err.println("Wrong index type");
                System.exit(-1);
        }

        try {
            index.createIndexIfNotExist();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }


        var importer = new Importer(index);

        var search = new Search(index, importer);

        var server = new TuapseServer(importer, search);

        //if mock, spider not working. But index not worked too :)
        if (!IndexType.equals("MOCK")) {
            Subprocess.spider()
                    .retry()
                    .onBackpressureBuffer(64, () -> logger.warn("Post process too slow!"),
                            BackpressureOverflowStrategy.DROP_LATEST)
                    //TODO test it
                    .flatMap(hash -> Subprocess.webtorrent(hash).subscribeOn(TuapseSchedulers.webtorrentSpider())
                                    .flatMap(t -> Single.fromCallable(() -> index.insertTorrent(t)))
                                    .toFlowable()
                                    .doOnError(throwable -> logger.warn(throwable.getMessage()))
                                    .onErrorResumeNext(Flowable.empty())
                            )
                    .timeout(60, TimeUnit.MINUTES) //restart spider if no insertion long time
                    .retryWhen(throwables -> throwables.delay(30, TimeUnit.SECONDS))
                    .subscribeOn(Schedulers.io())
                    .takeWhile(s -> index.isSpaceAllowed())
                    .subscribe(s -> logger.info("Inserted: " + s));
        }

        Observable.interval(1, TimeUnit.HOURS)
                .flatMap(aLong -> Observable.fromCallable(() -> Utils.recursiveDeleteFilesAcessedOlderThanNDays(1, Paths.get(Subprocess.WebTorrGenDir))))
                .subscribeOn(Schedulers.io())
                .subscribe(n -> logger.info("Deleted side effect generate torrent " + n + " files"));

        Observable.interval(1, TimeUnit.HOURS)
                .flatMap(aLong -> Observable.fromCallable(() -> Utils.recursiveDeleteFilesAcessedOlderThanNDays(1, Paths.get(Subprocess.WebTorrSpiderDir))))
                .subscribeOn(Schedulers.io())
                .subscribe(n -> logger.info("Deleted side effect spider " + n + " files"));

        Single.fromCallable(Subprocess::ipfsRoom).flatMapCompletable(ipfs -> {
            search.ipfsUpdate(ipfs);

            Completable waitExit = ipfs.waitExit().flatMapCompletable(e -> Completable.error(new Exception("Process dead")))
                    .subscribeOn(Schedulers.io());

            Completable p2pSearches = ipfs.distributedSearches()
                    .subscribeOn(Schedulers.io())
                    .onBackpressureBuffer(4, () -> logger.warn("Search to slow!"),
                            BackpressureOverflowStrategy.DROP_LATEST)
                    .flatMapSingle(req -> Single.fromCallable(() -> index.search(req.m.searchString, req.m.pageSize))
                            .flatMap(searchResult -> ipfs.searchAnswer(new TypedMessage<>(req.peerId, SearchAnswer.create(searchResult)))
                                    //TODO fix me, use completable
                                    .toSingleDefault("ok"))).ignoreElements();
            return Completable.merge(List.of(waitExit, p2pSearches));
        })
                //maybe another?
                .doOnError(e -> search.ipfsUpdate(new IpfsDisabled()))
                .retryWhen(throwables -> throwables.delay(5, TimeUnit.SECONDS))
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe();

        server.listen().subscribe(s -> logger.info("Server started at " + s.actualPort() + " port"));

    }
}
