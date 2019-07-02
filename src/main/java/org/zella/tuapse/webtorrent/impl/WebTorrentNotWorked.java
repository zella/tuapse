package org.zella.tuapse.webtorrent.impl;

import io.reactivex.Single;
import org.zella.tuapse.model.torrent.LiveTorrent;
import org.zella.tuapse.webtorrent.WebTorrentInterface;

public class WebTorrentNotWorked implements WebTorrentInterface {

    @Override
    public Single<LiveTorrent> webtorrent(String hash) {
        return Single.never();
    }
}
