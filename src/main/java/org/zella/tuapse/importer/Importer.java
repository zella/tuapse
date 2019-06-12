package org.zella.tuapse.importer;

import io.reactivex.Observable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.Runner;
import org.zella.tuapse.model.torrent.Torrent;
import org.zella.tuapse.storage.Index;
import org.zella.tuapse.subprocess.Subprocess;

import java.util.List;

public class Importer {

    private static final Logger logger = LoggerFactory.getLogger(Importer.class);


    private final Index index;

    public Importer(Index index) {
        this.index = index;
    }

    public Single<List<Torrent>> evalTorrentsData(List<String> hash) {
        return Observable.fromIterable(hash)
                .flatMap(h -> Subprocess.webtorrent(h).toObservable().onErrorResumeNext(Observable.empty()), Runner.WebtorrConcurency)
                .toList();
    }

    public Single<List<String>> importTorrents(List<Torrent> torrents) {
        return Observable.fromIterable(torrents)
                .flatMapSingle(t -> Single.fromCallable(() -> index.insertTorrent(t)).doOnSuccess(s -> logger.info("Imported: " + s))).toList();
    }
}
