package org.zella.tuapse.subprocess;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;

import java.io.IOException;

public class SubprocessTest {

    @Test
    public void webtorrentTest() throws InterruptedException {
        //TODO need super test, because it's main part for "live functionality"
        var webTorrent = Subprocess.webTorrentDaemon();
        webTorrent.waitExit()
                .subscribeOn(Schedulers.io())
                .subscribe();
        Thread.sleep(4000);
        System.out.println("Start");
        var torrent = webTorrent.webtorrent("9501161d97259b33a95d768d049ffc20591f1b0c").blockingGet();
        System.out.println(torrent);
        var torrent2 = webTorrent.webtorrent("9501161d97259b33a95d768d049ffc20591f1b0c").blockingGet();
        System.out.println(torrent2);
        var torrent3 = webTorrent.webtorrent("9501161d97259b33a95d768d049ffc20591f1b0c").blockingGet();
        System.out.println(torrent3);
        var torrent4 = webTorrent.webtorrent("9501161d97259b33a95d768d049ffc20591f1b0c").blockingGet();
        System.out.println(torrent4);
    }

}
