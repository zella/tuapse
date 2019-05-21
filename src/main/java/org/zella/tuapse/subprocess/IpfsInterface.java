package org.zella.tuapse.subprocess;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidmoten.rx2.Strings;
import com.github.zella.rxprocess2.PreparedStreams;
import io.reactivex.*;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.model.SearchRequest;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class IpfsInterface {

    private static final Logger logger = LoggerFactory.getLogger(IpfsInterface.class);

    private final PreparedStreams streams;

    private final Single<String> myPeer;

    private final Flowable<String> distributedSearchRequests;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Single<String> findEvent(String event) {
        return streams.stdOut().toFlowable(BackpressureStrategy.BUFFER)
                .compose(src -> Strings.decode(src, Charset.defaultCharset()))
                .compose(src -> Strings.split(src, System.lineSeparator()))
                .doOnNext(s -> logger.debug(s))
                .filter(s -> s.startsWith(event))
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

    public IpfsInterface(PreparedStreams streams) {
        this.streams = streams;
        myPeer = findEvent("[MyPeer]").cache();
        distributedSearchRequests = findEvents("[SearchRequest]");
        //TODO retry should work with all stdout/started callback, take care of rx-process2
        streams.waitDone().subscribeOn(Schedulers.computation()).subscribe();
    }

    public Single<List<String>> getPeers() {
        return Completable.fromRunnable(() -> streams.stdIn().onNext("[getPeers]".getBytes()))
                .andThen(findEvent("[Peers]").map(s -> objectMapper.readValue(s, new TypeReference<List<String>>() {
                })));
    }

    public Flowable<SearchRequest> distributedSearchRequests() {
        return distributedSearchRequests
                .map(s -> objectMapper.readValue(s, SearchRequest.class));
    }

    public Single<String> getMyPeer() {
        return myPeer;
    }

    public Completable searchAnswer(String peerId, String searchResult) {
        return Completable.fromRunnable(() -> streams.stdIn().onNext(("[searchAnswer]" + searchResult).getBytes()));
    }

    //TODO search

    public Single<String> searchAsk(String peerId, String search) {
        return Single.timer((int) (Math.random() * (10 - 1)) + 1, TimeUnit.SECONDS).map(t -> InetAddress.getLocalHost().getHostName());
    }


}
