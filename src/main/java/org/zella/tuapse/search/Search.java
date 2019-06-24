package org.zella.tuapse.search;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.Runner;
import org.zella.tuapse.search.filter.impl.FilesOnlyLuceneFilter;
import org.zella.tuapse.model.filter.FilteredTFile;
import org.zella.tuapse.model.filter.TFileWithMeta;
import org.zella.tuapse.importer.Importer;
import org.zella.tuapse.ipfs.P2pInterface;
import org.zella.tuapse.ipfs.impl.IpfsDisabled;
import org.zella.tuapse.model.index.FoundTorrent;
import org.zella.tuapse.model.torrent.LiveTorrent;
import org.zella.tuapse.model.torrent.StorableTorrent;
import org.zella.tuapse.providers.RxUtils;
import org.zella.tuapse.server.TuapseServer;
import org.zella.tuapse.storage.AbstractIndex;
import org.zella.tuapse.storage.Index;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Search {

    private static final Logger logger = LoggerFactory.getLogger(TuapseServer.class);

    private static final int MiminumSearchTime = Integer.parseInt(System.getenv().getOrDefault("SINGLE_SEARCH_WAIT_SEC", "10"));

    private final Index index;

    private final Importer importer;

    private AtomicReference<P2pInterface> ipfs = new AtomicReference<>(new IpfsDisabled());

    public Search(Index index, Importer importer) {
        this.index = index;
        this.importer = importer;
    }

    public void ipfsUpdate(P2pInterface search) {
        ipfs.set(search);
    }

    public Observable<List<FoundTorrent<StorableTorrent>>> searchNoPeersData(String text) {
        return Observable.merge(List.of(
                Single.fromCallable(() -> index.search(text)).toObservable(),
                ipfs.get().search(text, AbstractIndex.PageSize))
        )
                .serialize().compose(RxUtils.distinctSequence(t -> t.torrent.infoHash))
                .serialize()
                .flatMapIterable(s -> s)
                .map(List::of);
    }

    public Observable<List<FoundTorrent<LiveTorrent>>> search(String text) {
        return Observable.merge(List.of(
                Single.fromCallable(() -> index.search(text)).toObservable(),
                ipfs.get().search(text, AbstractIndex.PageSize))
        )
                .serialize()
                .doOnNext(r -> logger.debug("Search result before deduplicate: " + r.stream().map(t -> t.torrent.infoHash).collect(Collectors.joining("|"))))
                .compose(RxUtils.distinctSequence(t -> t.torrent.infoHash))
                .doOnNext(r -> logger.debug("Search result: " + r.stream().map(t -> t.torrent.infoHash).collect(Collectors.joining("|"))))
                .flatMap(ts -> importer.evalTorrentsData(ts.stream().map(t -> t.torrent.infoHash).collect(Collectors.toList()))
                        .subscribeOn(Schedulers.io())
                        .toList().toObservable().map(live -> FoundTorrent.fillWithPeers(ts, live)), Runner.WebtorrConcurency)
                .flatMapIterable(s -> s)
                .map(List::of)
                .doOnNext(r -> logger.debug("Search result with peers: " + r.stream().map(t -> t.torrent.name).collect(Collectors.joining("|"))));
    }

    public Single<FilteredTFile> searchSingle(String text, Optional<List<String>> exts) {
        return search(text).map(torrents -> {
            var filter = new FilesOnlyLuceneFilter(text, exts, 10);
            List<TFileWithMeta> filesWithMeta = torrents.stream().flatMap(t -> t.torrent.files.stream().map(f -> new TFileWithMeta(f, t.torrent.infoHash))).collect(Collectors.toList());
            return filter.selectFiles(filesWithMeta);
        })
                .flatMapIterable(f -> f)
                //TODO env
                .buffer(MiminumSearchTime, TimeUnit.SECONDS) // minimum search time. It's ok - if no items next buffer will in 10 * n, eg 10 20 30 sec search
                .filter(tf -> !tf.isEmpty())
                .map(tf -> tf.stream().sorted(Comparator.comparing(o -> o.score)).collect(Collectors.toList()))
                .firstOrError()
                .map(ts -> ts.get(0));
    }

    public Index getIndex() {
        return index;
    }

    public AtomicReference<P2pInterface> getIpfs() {
        return ipfs;
    }
}
