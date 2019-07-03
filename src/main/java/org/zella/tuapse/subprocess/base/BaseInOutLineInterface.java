package org.zella.tuapse.subprocess.base;

import com.github.davidmoten.rx2.Strings;
import com.github.zella.rxprocess2.Exit;
import com.github.zella.rxprocess2.PreparedStreams;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.observables.ConnectableObservable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

public abstract class BaseInOutLineInterface {


    private static final Logger logger = LoggerFactory.getLogger(BaseInOutLineInterface.class);

    protected final int eventWaitTimeout;

    protected final Observer<byte[]> stdIn;

    protected final Single<Exit> exit;

    protected final Flowable<String> lines;


    protected BaseInOutLineInterface(PreparedStreams streams, int eventWaitTimeout) {
        this.stdIn = streams.stdIn();
        //maybe we can use replay as cache for torrent evaluation
        ConnectableObservable<byte[]> stdout = streams.stdOut().replay(100, TimeUnit.MILLISECONDS);
        this.exit = streams.waitDone();
        this.lines = stdout.toFlowable(BackpressureStrategy.BUFFER)
                .compose(src -> Strings.decode(src, Charset.defaultCharset()))
                .compose(src -> Strings.split(src, System.lineSeparator()));
        this.eventWaitTimeout = eventWaitTimeout;
        stdout.connect();
    }


    protected Single<String> findEvent(String event) {
        return findEvents(event)
                .timeout(eventWaitTimeout, TimeUnit.SECONDS)
                .firstOrError();
    }

    protected Flowable<String> findEvents(String event) {
        return lines
                .doOnNext(l -> logger.trace("Event: " + l))
                .filter(s -> s.startsWith(event))
                .map(s -> s.substring(event.length()));
    }

    public Single<Exit> waitExit() {
        return exit;
    }

}
