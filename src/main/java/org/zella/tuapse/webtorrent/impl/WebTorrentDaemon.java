package org.zella.tuapse.webtorrent.impl;

import com.github.zella.rxprocess2.PreparedStreams;
import io.reactivex.Completable;
import io.reactivex.Single;
import org.zella.tuapse.model.torrent.LiveTorrent;
import org.zella.tuapse.providers.Json;
import org.zella.tuapse.subprocess.Subprocess;
import org.zella.tuapse.subprocess.base.BaseInOutLineInterface;
import org.zella.tuapse.webtorrent.WebTorrentInterface;

import java.util.concurrent.TimeUnit;

public class WebTorrentDaemon extends BaseInOutLineInterface implements WebTorrentInterface {


    public WebTorrentDaemon(PreparedStreams streams) {
        super(streams, 120);
    }


    public Single<LiveTorrent> webtorrent(String hash) {
        return Completable.fromRunnable(() -> stdIn.onNext((hash + System.lineSeparator()).getBytes()))
                .andThen(
                        findEvents("[torrent]")
                                .map(s -> Json.mapper.readValue(s, LiveTorrent.class))
                                .filter(t -> t.infoHash.equals(hash))
                                .firstOrError()
                ).timeout(Subprocess.WebTorrentTimeoutSec, TimeUnit.SECONDS);
    }

}
