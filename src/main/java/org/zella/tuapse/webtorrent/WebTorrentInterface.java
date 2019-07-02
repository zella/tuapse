package org.zella.tuapse.webtorrent;

import io.reactivex.Single;
import org.zella.tuapse.model.torrent.LiveTorrent;

public interface WebTorrentInterface {
    Single<LiveTorrent> webtorrent(String hash);
}
