package org.zella.tuapse.subprocess;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;

import java.util.List;

public class InfinityProcess {

    private RxProcess callbacks;

    private void init(List<String> cmd) {
        this.callbacks = RxProcess.fromCommand(cmd);
    }

    private Single<Integer> handleFailure(List<String> cmd) {
        init(cmd);
        return callbacks.onExit.onErrorResumeNext(e -> handleFailure(cmd));
    }

    public void start() {
        handleFailure(List.of("My command")).subscribe();
    }

    public Observable<byte[]> stdOut() {
        return callbacks.onStdOut;
    }

    public Observer<byte[]> stdIn() {
        return callbacks.stdIn;
    }
}
