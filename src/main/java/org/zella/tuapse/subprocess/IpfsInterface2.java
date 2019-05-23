package org.zella.tuapse.subprocess;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidmoten.rx2.Strings;
import com.github.zella.rxprocess2.Exit;
import com.github.zella.rxprocess2.PreparedStreams;
import com.github.zella.rxprocess2.RxNuProcessBuilder;
import com.github.zella.rxprocess2.errors.ProcessException;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.model.messages.Message;
import org.zella.tuapse.model.messages.TypedMessage;
import org.zella.tuapse.model.messages.impl.SearchAnswer;
import org.zella.tuapse.model.messages.impl.SearchAsk;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class IpfsInterface2 {

    private static final Logger logger = LoggerFactory.getLogger(IpfsInterface2.class);
    //TODO single instance
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String ipfsRoomExec;

    private PreparedStreams streams;

    private Single<String> myPeer;

    private Flowable<Message> messages;


    private Single<String> findEvent(String event) {
        return streams.stdOut().toFlowable(BackpressureStrategy.BUFFER)
                .compose(src -> Strings.decode(src, Charset.defaultCharset()))
                .compose(src -> Strings.split(src, System.lineSeparator()))
                .doOnNext(s -> logger.debug(s))
                .filter(s -> s.startsWith(event))
                // .replay(1, TimeUnit.SECONDS) //TODO?
                .timeout(60, TimeUnit.SECONDS)//TODO
                .firstOrError();
    }

    private Flowable<String> findEvents(String event) {
        return streams.stdOut().toFlowable(BackpressureStrategy.BUFFER)
                .compose(src -> Strings.decode(src, Charset.defaultCharset()))
                .compose(src -> Strings.split(src, System.lineSeparator()))
                .doOnNext(s -> logger.debug(s))
                .filter(s -> s.startsWith(event));
    }


    private Single<Exit> stackNotSafe(String ipfsRoomExec) {
        init(ipfsRoomExec);
        return streams.waitDone()
                .flatMap(exit -> Single.<Exit>error(exit.err.orElse(new ProcessException(-1, "Process exits without failure"))))
                .onErrorResumeNext(e -> {
                    init(ipfsRoomExec);
                    return stackNotSafe(ipfsRoomExec);
                });
    }

    IpfsInterface2(String ipfsRoomExec) {
        this.ipfsRoomExec = ipfsRoomExec;
    }

    public void start() {
        stackNotSafe(ipfsRoomExec)
                .subscribeOn(Schedulers.computation()).subscribe();
    }

    private void init(String ipfsRoomExec) {
        List<String> cmd = List.of("node", ipfsRoomExec);
        this.streams = RxNuProcessBuilder.fromCommand(cmd).asStdInOut();
        this.myPeer = findEvent("[MyPeer]").cache();
        this.messages = findEvents("[Message]").map(s -> objectMapper.readValue(s, Message.class));
    }

    public Single<List<String>> getPeers() {
        return Completable.fromRunnable(() -> streams.stdIn().onNext("[GetPeers]".getBytes()))
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
        return Completable.fromRunnable(() -> streams.stdIn().onNext(("[SearchAnswer]" + m.toJsonString()).getBytes()));
    }

    public Single<TypedMessage<SearchAnswer>> searchAsk(TypedMessage<SearchAsk> m) {
        return Completable.fromRunnable(() -> streams.stdIn().onNext(("[SearchAsk]" + m.toJsonString()).getBytes()))
                .andThen(findEvent("[Peers]").map(s -> objectMapper.readValue(s, new TypeReference<TypedMessage<SearchAnswer>>() {
                })));
    }


}
