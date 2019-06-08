package org.zella.tuapse.importer;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.Runner;
import org.zella.tuapse.storage.Index;
import org.zella.tuapse.subprocess.Subprocess;

import java.util.List;

public class Importer {

    private static final Logger logger = LoggerFactory.getLogger(Importer.class);


    private final Index index;

    public Importer(Index index) {
        this.index = index;
    }

    private Single<String> importTorrent(String hash) {
        return Subprocess.webtorrent(hash)
                .flatMap(t -> Single.fromCallable(() -> index.insertTorrent(t)).doOnSuccess(s -> logger.info("Imported: " + s)));
    }

    public Single<List<String>> importTorrents(List<String> hash) {
        return Observable.fromIterable(hash).flatMap(h -> importTorrent(h)
                .toObservable(), Runner.WebtorrConcurency)
                .onErrorResumeNext(Observable.empty())
                .toList();
    }
}
