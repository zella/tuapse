package org.zella.tuapse.ipfs.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidmoten.rx2.Strings;
import com.github.zella.rxprocess2.Exit;
import com.github.zella.rxprocess2.PreparedStreams;
import io.reactivex.*;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.ipfs.IpfsSearch;
import org.zella.tuapse.model.es.FoundTorrent;
import org.zella.tuapse.model.messages.Message;
import org.zella.tuapse.model.messages.TypedMessage;
import org.zella.tuapse.model.messages.impl.SearchAnswer;
import org.zella.tuapse.model.messages.impl.SearchAsk;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class IpfsInterface implements IpfsSearch {

    private static final Logger logger = LoggerFactory.getLogger(IpfsInterface.class);

    private final PreparedStreams streams;

    private final Single<String> myPeer;

    private final Flowable<Message> messages;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Single<String> findEvent(String event) {
        return streams.stdOut().toFlowable(BackpressureStrategy.BUFFER)
                .compose(src -> Strings.decode(src, Charset.defaultCharset()))
                .compose(src -> Strings.split(src, System.lineSeparator()))
                .doOnNext(s -> logger.debug(s))
                .filter(s -> s.startsWith(event))
                .map(s -> s.substring(event.length()))
                // .replay(1, TimeUnit.SECONDS) //TODO?
                .timeout(60, TimeUnit.SECONDS)//TODO
                .firstOrError();
    }

    private Flowable<String> findEvents(String event) {
        return streams.stdOut().toFlowable(BackpressureStrategy.BUFFER)
                .compose(src -> Strings.decode(src, Charset.defaultCharset()))
                .compose(src -> Strings.split(src, System.lineSeparator()))
                .doOnNext(s -> logger.debug(s))
                .filter(s -> s.startsWith(event))
                .map(s -> s.substring(event.length()))
                .serialize();
    }

    public IpfsInterface(PreparedStreams streams) {
        this.streams = streams;
        myPeer = findEvent("[MyPeer]").cache().subscribeOn(Schedulers.io());
        myPeer.subscribe();
        messages = findEvents("[Message]").map(s -> objectMapper.readValue(s, Message.class));
        messages.subscribe();
    }

    public Single<Exit> waitExit() {
        return this.streams.waitDone();
    }

    public Single<List<String>> getPeers() {
        return Completable.fromRunnable(() -> streams.stdIn().onNext(("[GetPeers]" + System.lineSeparator()).getBytes()))
                //here we need replay in small time window
                .andThen(findEvent("[Peers]").map(s -> objectMapper.readValue(s, new TypeReference<List<String>>() {
                })));
    }

    private Predicate<Message> byType(String type) {
        return m -> objectMapper.readTree(m.data).get("type").asText().equals(type);
    }

    public Flowable<TypedMessage<SearchAsk>> distributedSearches() {
        return messages
                .filter(byType("searchAsk"))
                //double parse, but don't care for now
                .map(m -> new TypedMessage<>(m.peerId, objectMapper.readValue(m.data, SearchAsk.class)));
    }

    public Single<String> getMyPeer() {
        return myPeer;
    }

    public Completable searchAnswer(TypedMessage<SearchAnswer> m) {
        return Completable.fromRunnable(() -> streams.stdIn().onNext(("[SearchAnswer]" + m.toJsonString() + System.lineSeparator()).getBytes()));
    }


    private Single<TypedMessage<SearchAnswer>> searchAsk(TypedMessage<SearchAsk> m) {
        return Completable.fromRunnable(() -> streams.stdIn().onNext(("[SearchAsk]" + m.toJsonString() + System.lineSeparator()).getBytes()))
                .andThen(findEvent("[Peers]").map(s -> objectMapper.readValue(s, new TypeReference<TypedMessage<SearchAnswer>>() {
                })));
    }

//    @Override
//    public Observable<List<FoundTorrent>> search(String text) {
//        return getPeers()..doOnSuccess(strings -> logger.debug("HERE2")).flatMapObservable(strings -> {
//            return Observable.empty();
//        });
//    }

//    @Override
//    public Observable<List<FoundTorrent>> search2(String text) {
//        return getMyPeer().zipWith(getPeers().doOnError(e -> logger.error("SearchAsk failed", e)), (iam, they) -> {
//                    Collections.shuffle(they);
//                    //ask 8 peers
//                    return they.stream().filter(s -> !s.equals(iam)).limit(8).collect(Collectors.toList());
//                }
//        ).flattenAsObservable(strings -> strings)
//                //search timeout
//                .flatMap(peer -> searchAsk(new TypedMessage<>(peer, new SearchAsk(text))).map(a -> a.m.torrents)
//                        //TODO env
//                        .timeout(10, TimeUnit.SECONDS).toObservable().onErrorResumeNext(Observable.empty()), 4);
//
//
//    }

    @Override
    public Observable<List<FoundTorrent>> search(String text) {
        return getMyPeer().zipWith(getPeers().doOnError(e -> logger.error("SearchAsk failed", e)), (iam, they) -> {
                    Collections.shuffle(they);
                    //ask 8 peers
                    return they.stream().filter(s -> !s.equals(iam)).limit(8).collect(Collectors.toList());
                }
        )
//                .doOnTerminate(() -> logger.debug("TERMINATED DING"))
//
                .toObservable()
                .flatMapIterable(strings -> strings)
                //search timeout
                .flatMap(peer -> searchAsk(new TypedMessage<>(peer, new SearchAsk(text))).map(a -> a.m.torrents)
                        //TODO env
                        .timeout(10, TimeUnit.SECONDS).toObservable().onErrorResumeNext(Observable.empty()), 4);


    }



}
