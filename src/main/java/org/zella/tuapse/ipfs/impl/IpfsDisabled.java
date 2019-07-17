package org.zella.tuapse.ipfs.impl;

import io.reactivex.Observable;
import io.reactivex.Single;
import org.zella.tuapse.ipfs.P2pInterface;
import org.zella.tuapse.model.index.FoundTorrent;
import org.zella.tuapse.model.torrent.StorableTorrent;
import org.zella.tuapse.search.SearchMode;

import java.util.List;

public class IpfsDisabled implements P2pInterface {

    @Override
    public Single<IpfsMeta> getPeers() {
        return Single.just(new IpfsMeta(List.of()));
    }

    @Override
    public Observable<List<FoundTorrent<StorableTorrent>>> search(String text, SearchMode mode, int pageSize) {
        return Observable.empty();
    }

}
