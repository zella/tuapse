package org.zella.tuapse;

import io.reactivex.BackpressureOverflowStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.es.Es;
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
                .observeOn(Schedulers.computation())
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
        .takeWhile(s -> es.isSpaceAllowed())
        ;
        //  .subscribe(s -> logger.info("Inserted: " + s));


        //TODO handle failure or process exit...
        var ipfs = Subprocess.ipfsRoom();
        ipfs.getMyPeer().subscribeOn(Schedulers.computation())
                .subscribe(p -> System.out.println(p));
        ipfs.getMyPeer().subscribeOn(Schedulers.computation())
                .subscribe(p -> System.out.println(p));
        ipfs.getMyPeer().subscribeOn(Schedulers.computation())
                .subscribe(p -> System.out.println(p));
        ipfs.getMyPeer().subscribeOn(Schedulers.computation())
                .subscribe(p -> System.out.println(p));


        ipfs.distributedSearchRequests()
                .onBackpressureBuffer(4, () -> logger.warn("Search to slow!"),
                        BackpressureOverflowStrategy.DROP_LATEST)
                .flatMapSingle(req -> es.search(req.searchString)
                        .flatMap(searchResult -> ipfs.searchAnswer(req.peerId, searchResult).toSingleDefault("ok")))
                .retry()
                //TODO correct? if all work, try to completable
                .subscribe();


        Scanner input = new Scanner(System.in);
//                    System.out.println("Type text and press enter to search");
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
                        //seaarh timeout
                        .flatMap(peer -> ipfs.searchAsk(peer, text).timeout(20, TimeUnit.SECONDS).toFlowable(), 4)
                        .toList().blockingGet();
                System.out.println(searches);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


//                .subscribeOn(Schedulers.computation())
//                .observeOn(Schedulers.computation())
//                //TODO test retry on process exits
//                .retry()
//                .subscribe(ipfs -> {
//                    ipfs.distributedSearchRequests()
//                            .onBackpressureBuffer(4, () -> logger.warn("Search to slow!"),
//                                    BackpressureOverflowStrategy.DROP_LATEST)
//                            .flatMapSingle(req -> es.search(req.searchString)
//                                    .flatMap(searchResult -> ipfs.searchAnswer(req.peerId, searchResult).toSingleDefault("ok")))
//                            .retry()
//                            //TODO correct? if all work, try to completable
//                            .subscribe();
//
//                    Scanner input = new Scanner(System.in);
//                    System.out.println("Type text and press enter to search");
//                    //off logging
//                    while (input.hasNextLine()) {
//                        var text = input.nextLine();
//                        try {
//                            var searches = ipfs.getMyPeer().zipWith(ipfs.getPeers(), (iam, they) -> {
//                                        Collections.shuffle(they);
//                                        //ask 8 peers
//                                        return they.stream().filter(s -> !s.equals(iam)).limit(8).collect(Collectors.toList());
//                                    }
//                            ).flattenAsFlowable(strings -> strings)
//                                    //seaarh timeout
//                                    .flatMap(peer -> ipfs.searchAsk(peer, text).timeout(20, TimeUnit.SECONDS).toFlowable(), 4)
//                                    .toList().blockingGet();
//                            System.out.println(searches);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });
    }
}
