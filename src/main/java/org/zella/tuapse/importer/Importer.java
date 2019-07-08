package org.zella.tuapse.importer;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import org.zella.tuapse.model.torrent.LiveTorrent;
import org.zella.tuapse.model.torrent.StorableTorrent;

import java.util.List;

public interface Importer {
    Observable<LiveTorrent> evalTorrentsData(List<String> hash, Scheduler sc);

    Single<List<String>> importTorrents(List<StorableTorrent> torrents);
}
