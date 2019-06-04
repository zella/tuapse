package org.zella.tuapse.ipfs.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.davidmoten.rx2.Strings;
import com.github.zella.rxprocess2.Exit;
import com.github.zella.rxprocess2.PreparedStreams;
import io.reactivex.*;
import io.reactivex.functions.Predicate;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.ipfs.P2pInterface;
import org.zella.tuapse.model.es.FoundTorrent;
import org.zella.tuapse.model.messages.Message;
import org.zella.tuapse.model.messages.TypedMessage;
import org.zella.tuapse.model.messages.impl.SearchAnswer;
import org.zella.tuapse.model.messages.impl.SearchAsk;
import org.zella.tuapse.providers.Json;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class IpfsInterface implements P2pInterface {

    private static final Logger logger = LoggerFactory.getLogger(IpfsInterface.class);

    private static final int P2pSearchTimeout = Integer.parseInt(System.getenv().getOrDefault("P2P_SEARCH_TIMEOUT_SEC", "30"));

    private static final int P2pSearches = 8;

    private static final int EventWaitTimeout = 8;

    private final Observer<byte[]> stdIn;

    private final ConnectableObservable<byte[]> stdout;

    private final Single<Exit> exit;

    private final Single<String> myPeer;

    private final Flowable<Message> messages;

    private Single<String> findEvent(String event) {
        return stdout.toFlowable(BackpressureStrategy.BUFFER)
                .compose(src -> Strings.decode(src, Charset.defaultCharset()))
                .compose(src -> Strings.split(src, System.lineSeparator()))
                .filter(s -> s.startsWith(event))
                .map(s -> s.substring(event.length()))
                .timeout(EventWaitTimeout, TimeUnit.SECONDS)
                .firstOrError();
    }

    private Flowable<String> findEvents(String event) {
        return stdout.toFlowable(BackpressureStrategy.BUFFER)
                .compose(src -> Strings.decode(src, Charset.defaultCharset()))
                .compose(src -> Strings.split(src, System.lineSeparator()))
                .filter(s -> s.startsWith(event))
                .map(s -> s.substring(event.length()))
                .serialize();
    }

    public IpfsInterface(PreparedStreams streams) {
        this.stdIn = streams.stdIn();
        this.exit = streams.waitDone();
        this.stdout = streams.stdOut().replay(100, TimeUnit.MILLISECONDS);
        myPeer = findEvent("[MyPeer]").cache().subscribeOn(Schedulers.io()).doOnSuccess(logger::info);
        myPeer.subscribe();
        messages = findEvents("[Message]").subscribeOn(Schedulers.io())
                .map(s -> Json.mapper.readValue(s, Message.class));
        messages.subscribe();
        stdout.connect();
    }

    public Single<Exit> waitExit() {
        return this.exit;
    }

    public Single<IpfsMeta> getPeers() {
        return Completable.fromRunnable(() -> this.stdIn.onNext(("[GetPeers]" + System.lineSeparator()).getBytes()))
                //here we need replay in small time window, see streams.stdOut().replay above
                .andThen(findEvent("[Peers]")
                        .doOnSuccess(s -> logger.debug("Peers: " + s))
                        .map(s -> new IpfsMeta(Json.mapper.readValue(s, new TypeReference<List<String>>() {
                        }))));
    }

    private Predicate<Message> byType(String type) {
        return m -> Json.mapper.readTree(m.data).get("type").asText().equals(type);
    }

    public Flowable<TypedMessage<SearchAsk>> distributedSearches() {
        return messages
                .filter(byType("searchAsk"))
                //double parse, but don't care for now
                .map(m -> new TypedMessage<>(m.peerId, Json.mapper.readValue(m.data, SearchAsk.class)));
    }

    public Single<String> getMyPeer() {
        return myPeer;
    }

    public Completable searchAnswer(TypedMessage<SearchAnswer> m) {
        return Completable.fromRunnable(() -> this.stdIn.onNext(("[SearchAnswer]" + m.toJsonString() + System.lineSeparator()).getBytes()));
    }


    private Single<TypedMessage<SearchAnswer>> searchAsk(TypedMessage<SearchAsk> m) {
        return Completable.fromRunnable(() -> this.stdIn.onNext(("[SearchAsk]" + m.toJsonString() + System.lineSeparator()).getBytes()))
                .andThen(findEvent("[Peers]").map(s -> Json.mapper.readValue(s, new TypeReference<TypedMessage<SearchAnswer>>() {
                })));
    }

    @Override
    public Observable<List<FoundTorrent>> search(String text) {
        return getMyPeer().zipWith(getPeers().doOnError(e -> logger.error("SearchAsk failed", e)), (iam, they) -> {
                    Collections.shuffle(they.peers);
                    logger.debug("My peer id and peers evaluated");
                    //ask 8 peers
                    return they.peers.stream().filter(s -> !s.equals(iam)).limit(P2pSearches).collect(Collectors.toList());
                }
        ).toObservable()
                .flatMapIterable(strings -> strings)
                //search timeout
                .flatMap(peer -> searchAsk(new TypedMessage<>(peer, new SearchAsk(text))).map(a -> a.m.torrents)
                        .timeout(P2pSearchTimeout, TimeUnit.SECONDS).toObservable().onErrorResumeNext(Observable.empty()), 4);


    }
}

//TODO if multiple users will be used this, we need room dividing mechanism.
//Something like, room per 16 users - need investigation. For deep search user connects to different room and ask peers.
//Basically all users connects to first constant run. If users >= 16 try connect to first room + 1 etc
