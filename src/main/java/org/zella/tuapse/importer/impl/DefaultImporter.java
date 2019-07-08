package org.zella.tuapse.importer.impl;

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

import java.util.List;

public class DefaultImporter implements Importer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultImporter.class);

    private final Index index;

    public DefaultImporter(Index index) {
        this.index = index;
    }

    @Override
    public Observable<LiveTorrent> evalTorrentsData(List<String> hash, Scheduler sc) {
        return Observable.fromIterable(hash)
                .flatMap(h -> Subprocess.webtorrent(h)
                        .subscribeOn(sc)
                        .toObservable().onErrorResumeNext(Observable.empty()));
    }

    @Override
    public Single<List<String>> importTorrents(List<StorableTorrent> torrents) {
        return Observable.fromIterable(torrents)
                .flatMapSingle(t -> Single.fromCallable(() -> index.insertTorrent(t)).doOnSuccess(s -> logger.info("Imported: " + s))).toList();
    }
}
