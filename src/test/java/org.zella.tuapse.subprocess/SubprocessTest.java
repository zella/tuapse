package org.zella.tuapse.subprocess;

import io.reactivex.Scheduler;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;
import org.zella.tuapse.model.torrent.LiveTorrent;

import java.io.IOException;

public class SubprocessTest {

    @Test
    public void webtorrentSequentiallyAddSameTorrents() throws InterruptedException {
        //TODO need super test, because it's main part for "live functionality"
        var webTorrent = Subprocess.webTorrentDaemon();
        webTorrent.waitExit()
                .subscribeOn(Schedulers.io())
                .subscribe();
        System.out.println("Start");
        var torrent = webTorrent.webtorrent("9501161d97259b33a95d768d049ffc20591f1b0c").blockingGet();
        assert  torrent.numPeers > 0;
        System.out.println(torrent);
        var torrent2 = webTorrent.webtorrent("9501161d97259b33a95d768d049ffc20591f1b0c").blockingGet();
        assert  torrent2.numPeers > 0;
        System.out.println(torrent2);
        var torrent3 = webTorrent.webtorrent("9501161d97259b33a95d768d049ffc20591f1b0c").blockingGet();
        assert  torrent3.numPeers > 0;
        System.out.println(torrent3);
        var torrent4 = webTorrent.webtorrent("9501161d97259b33a95d768d049ffc20591f1b0c").blockingGet();
        assert  torrent4.numPeers > 0;
        System.out.println(torrent4);
    }

    @Test
    public void webtorrentAsyncAddSameTorrent() throws InterruptedException {
        var webTorrent = Subprocess.webTorrentDaemon();
        webTorrent.waitExit()
                .subscribeOn(Schedulers.io())
                .subscribe();

        var torrent1 = webTorrent.webtorrent("9501161d97259b33a95d768d049ffc20591f1b0c");
        var torrent2 = webTorrent.webtorrent("9501161d97259b33a95d768d049ffc20591f1b0c");
        var torrent3 = webTorrent.webtorrent("9501161d97259b33a95d768d049ffc20591f1b0c");

        TestObserver<LiveTorrent> obs1 = new TestObserver<>();
        torrent1.subscribe(obs1);
        TestObserver<LiveTorrent> obs2 = new TestObserver<>();
        torrent2.subscribe(obs2);
        TestObserver<LiveTorrent> obs3 = new TestObserver<>();
        torrent3.subscribe(obs3);

        Thread.sleep(8000);

        obs1.assertValueCount(1);
        assert  obs1.values().get(0).numPeers > 0;
        obs2.assertValueCount(1);
        assert  obs2.values().get(0).numPeers > 0;
        obs3.assertValueCount(1);
        assert  obs3.values().get(0).numPeers > 0;

    }


}
