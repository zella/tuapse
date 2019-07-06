package org.zella.tuapse.search;

import io.reactivex.Observable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.providers.TuapseSchedulers;
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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Search {

    private static final Logger logger = LoggerFactory.getLogger(TuapseServer.class);

    private static final int MiminumSearchTime = Integer.parseInt(System.getenv().getOrDefault("FILE_SEARCH_STEP_SEC", "10"));

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

    public Observable<List<FoundTorrent<StorableTorrent>>> searchNoEvalPeers(String text) {
        return Observable.merge(List.of(
                Single.fromCallable(() -> index.search(text)).toObservable(),
                ipfs.get().search(text, AbstractIndex.PageSize))
        )
                .serialize()
                .compose(RxUtils.distinctSequence(t -> t.torrent.infoHash))
                .flatMapIterable(s -> s)
                .map(List::of);
    }

    public Single<FilteredTFile> searchFileEvalPeers(String text, Optional<List<String>> exts, int minimumPeers) {
        return searchNoEvalPeers(text).map(torrents -> {
            var filter = new FilesOnlyLuceneFilter(text, exts, 10);
            List<TFileWithMeta> filesWithMeta = torrents.stream().flatMap(t -> t.torrent.files.stream().map(f -> new TFileWithMeta(f, t.torrent.infoHash))).collect(Collectors.toList());
            return filter.selectFiles(filesWithMeta);
        }).flatMap(files -> {
            logger.debug("Found files: " + files.size());
            var hashes = files.stream().map(f -> f.fileWithMeta.hash).distinct().collect(Collectors.toList());
            return importer.evalTorrentsData(hashes, TuapseSchedulers.webtorrentSearch())
                    .filter(liveTorrent -> liveTorrent.numPeers >= minimumPeers)
                    .map(liveTorrent -> files.stream().filter(f -> liveTorrent.infoHash.equals(f.fileWithMeta.hash)).collect(Collectors.toList()))
                    .filter(f -> !f.isEmpty())
                    .doOnNext(withPeers -> logger.debug("Files with peers: " + withPeers.size()));
        }).buffer(MiminumSearchTime, TimeUnit.SECONDS) // minimum searchEvalPeers time. It's ok - if no items next buffer will in 10 * n, eg 10 20 30 sec searchEvalPeers
                .map(buffer -> Observable.fromIterable(buffer).flatMapIterable(b -> b).toList().blockingGet())
                .filter(files -> !files.isEmpty())
                .map(tf -> tf.stream().sorted(Comparator.comparing((FilteredTFile o) -> o.score).reversed()).collect(Collectors.toList()))
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
