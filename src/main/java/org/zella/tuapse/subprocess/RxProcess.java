package org.zella.tuapse.subprocess;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;

import java.util.List;

public class RxProcess {
    public Single<Integer> onExit;
    public Single<Process> onStart;
    public Observer<byte[]> stdIn;
    public Observable<byte[]> onStdOut;


    public static RxProcess fromCommand(List<String> cmd) {
        //complicated details hidden
        return new RxProcess();
    }
}