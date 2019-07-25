package org.zella.tuapse.importer.impl;

import com.github.zella.rxprocess2.errors.ProcessTimeoutException;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.importer.Importer;
import org.zella.tuapse.model.torrent.LiveTorrent;
import org.zella.tuapse.model.torrent.StorableTorrent;
import org.zella.tuapse.storage.Index;
import org.zella.tuapse.subprocess.Subprocess;
import org.zella.tuapse.webtorrent.WebTorrentInterface;

import java.util.List;
import java.util.stream.Collectors;

public class DefaultImporter implements Importer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultImporter.class);

    private final Index index;

    private final WebTorrentInterface webtorrent;

    public DefaultImporter(Index index, WebTorrentInterface webtorrent) {
        this.index = index;
        this.webtorrent = webtorrent;
    }

    @Override
    public Observable<LiveTorrent> evalTorrentsData(List<String> hash, Scheduler sc) {
        return Observable.fromIterable(hash)
                .flatMap(h -> webtorrent.webtorrent(h)
                        .doOnSuccess(t -> logger.debug("Peers: [" + t.numPeers + "] " + t.name))
                        .subscribeOn(sc)
                        .toObservable()
                        .doOnError(e -> {
                            if (!(e instanceof ProcessTimeoutException)) logger.error("Error eval torrent: ", e);
                        })
                        .onErrorResumeNext(Observable.empty()));
    }

    @Override
    public Single<List<String>> importTorrents(List<StorableTorrent> torrents) {

        return Completable.fromRunnable(() -> index.insertTorrents(torrents))
                .andThen(Single.fromCallable((() -> torrents.stream().map(t -> t.infoHash).collect(Collectors.toList()))))
                .doOnSuccess(s -> logger.info("Imported: " + s.size()));
    }
}
