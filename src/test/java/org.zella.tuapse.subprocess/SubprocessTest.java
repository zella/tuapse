package org.zella.tuapse.subprocess;

import org.junit.Test;

import java.io.IOException;

public class SubprocessTest {

    @Test
    public void webtorrentTest() throws IOException {
        var torrent = Subprocess.webtorrent("9501161d97259b33a95d768d049ffc20591f1b0c").blockingGet();
        System.out.println(torrent);
    }

}
