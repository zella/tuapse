package org.zella.tuapse.core;

import io.reactivex.BackpressureOverflowStrategy;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.subprocess.Subprocess;

import java.util.Scanner;

public class Runner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) {
        Subprocess.spider()
                .subscribeOn(Schedulers.computation()).retry()
                .onBackpressureBuffer(128, () -> logger.warn("Webtorrent post process too slow!"),
                        BackpressureOverflowStrategy.DROP_LATEST)

                .flatMap(hash -> Subprocess.webtorrent(hash).toFlowable(), 2)
                .subscribe(
                        exit -> exit.err.ifPresent(e -> logger.info("Webtorrent: " + exit.statusCode, e)),
                        e -> logger.error("Error", e)
                );


        Scanner input = new Scanner(System.in);
        System.out.println("Press Enter to quit...");
        input.nextLine();
    }
}
