package org.zella.tuapse.importer;

import io.reactivex.Observable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.Runner;
import org.zella.tuapse.model.torrent.StorableTorrent;
import org.zella.tuapse.model.torrent.LiveTorrent;
import org.zella.tuapse.storage.Index;
import org.zella.tuapse.subprocess.Subprocess;

import java.util.List;

public class Importer {

    private static final Logger logger = LoggerFactory.getLogger(Importer.class);


    private final Index index;

    public Importer(Index index) {
        this.index = index;
    }

    public Observable<LiveTorrent> evalTorrentsData(List<String> hash) {
        return Observable.fromIterable(hash)
                //TODO remove Concurency, just use special scheduler
                .flatMap(h -> Subprocess.webtorrent(h).toObservable().onErrorResumeNext(Observable.empty()), Runner.WebtorrConcurency);
    }

    public Single<List<String>> importTorrents(List<StorableTorrent> torrents) {
        return Observable.fromIterable(torrents)
                .flatMapSingle(t -> Single.fromCallable(() -> index.insertTorrent(t)).doOnSuccess(s -> logger.info("Imported: " + s))).toList();
    }
}
