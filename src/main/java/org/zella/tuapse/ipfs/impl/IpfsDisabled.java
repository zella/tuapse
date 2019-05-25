package org.zella.tuapse.ipfs.impl;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import org.zella.tuapse.ipfs.IpfsSearch;
import org.zella.tuapse.model.es.FoundTorrent;

import java.util.List;

public class IpfsDisabled implements IpfsSearch {

    @Override
    public Observable<List<FoundTorrent>> search(String text) {
        return Observable.empty();
    }
}
