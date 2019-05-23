package org.zella.tuapse.subprocess;

public class Main {
    public static void main(String[] args) {
        var p = new InfinityProcess();
        p.start();

        p.stdIn().onNext("to process".getBytes());
        p.stdOut().subscribe(b -> {
        });
    }
}
