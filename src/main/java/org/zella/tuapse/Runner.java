package org.zella.tuapse;

import io.reactivex.BackpressureOverflowStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.es.Es;
import org.zella.tuapse.model.messages.impl.SearchAnswer;
import org.zella.tuapse.model.messages.impl.SearchAsk;
import org.zella.tuapse.model.messages.TypedMessage;
import org.zella.tuapse.subprocess.Subprocess;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Runner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) throws IOException {

        var es = new Es();
        es.createIndexIfNotExist();


        //TODO collect until configurable disk space allows
        Subprocess.spider()

                .retry()
                .onBackpressureBuffer(128, () -> logger.warn("Post process too slow!"),
                        BackpressureOverflowStrategy.DROP_LATEST)
                .flatMap(hash -> Subprocess.webtorrent(hash)
                                .flatMap(t -> Single.fromCallable(() -> es.insertTorrent(t)))
                                .toFlowable()
                                .doOnError(throwable -> logger.warn(throwable.getMessage()))
                                .onErrorResumeNext(Flowable.empty())
                        , 2)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .takeWhile(s -> es.isSpaceAllowed())
        .subscribe(s -> logger.info("Inserted: " + s));


        Single.fromCallable(Subprocess::ipfsRoom).flatMapCompletable(ipfs -> {
            Completable waitDone = ipfs.waitExit().flatMapCompletable(e -> Completable.error(new Exception("Process dead")));
            Completable p2pSearches = ipfs.distributedSearches()
                    .subscribeOn(Schedulers.computation())
                    .onBackpressureBuffer(4, () -> logger.warn("Search to slow!"),
                            BackpressureOverflowStrategy.DROP_LATEST)
                    .flatMapSingle(req -> Single.fromCallable(() -> es.search(req.m.searchString))
                            .flatMap(searchResult -> ipfs.searchAnswer(new TypedMessage<>(req.peerId, SearchAnswer.create(searchResult)))
                                    .toSingleDefault("ok"))).ignoreElements();
            Completable mySearch = Completable.fromAction(() -> {
                Scanner input = new Scanner(System.in);
                System.out.println("Type text and press enter to search");
                //off logging
                while (input.hasNextLine()) {
                    var text = input.nextLine();
                    try {
                        var searches = ipfs.getMyPeer().zipWith(ipfs.getPeers(), (iam, they) -> {
                                    Collections.shuffle(they);
                                    //ask 8 peers
                                    return they.stream().filter(s -> !s.equals(iam)).limit(8).collect(Collectors.toList());
                                }
                        ).flattenAsFlowable(strings -> strings)
                                //search timeout
                                .flatMap(peer -> ipfs.searchAsk(new TypedMessage<>(peer, new SearchAsk(text)))
                                        .timeout(20, TimeUnit.SECONDS).toFlowable().onErrorResumeNext(Flowable.empty()), 4)
                                .toList().blockingGet().stream()
                                .map(m -> m.m.torrents.stream().map(Object::toString)).collect(Collectors.toList());
                        System.out.println(searches);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).subscribeOn(Schedulers.newThread());

            return Completable.merge(List.of(waitDone, p2pSearches, mySearch));
        })
                .retryWhen(throwables -> throwables.delay(10, TimeUnit.SECONDS))
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe();

    }
}
