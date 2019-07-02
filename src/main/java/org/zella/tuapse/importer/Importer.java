package org.zella.tuapse.importer;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.model.torrent.StorableTorrent;
import org.zella.tuapse.model.torrent.LiveTorrent;
import org.zella.tuapse.storage.Index;
import org.zella.tuapse.webtorrent.WebTorrentInterface;
import org.zella.tuapse.webtorrent.impl.WebTorrentNotWorked;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Importer {

    private static final Logger logger = LoggerFactory.getLogger(Importer.class);

    private final Index index;

    private AtomicReference<WebTorrentInterface> webTorrent = new AtomicReference<>(new WebTorrentNotWorked());

    public Importer(Index index) {
        this.index = index;
    }

    public Observable<LiveTorrent> evalTorrentsData(List<String> hash, Scheduler sc) {
        return Observable.fromIterable(hash)
                .flatMap(h -> webTorrent.get().webtorrent(h)
                        .subscribeOn(sc)
                        .toObservable().onErrorResumeNext(Observable.empty()));
    }

    public Single<List<String>> importTorrents(List<StorableTorrent> torrents) {
        return Observable.fromIterable(torrents)
                .flatMapSingle(t -> Single.fromCallable(() -> index.insertTorrent(t)).doOnSuccess(s -> logger.info("Imported: " + s))).toList();
    }


    public void webTorrentUpdate(WebTorrentInterface t) {
        webTorrent.set(t);
    }
}
