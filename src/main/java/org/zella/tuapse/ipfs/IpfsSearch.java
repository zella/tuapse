package org.zella.tuapse.ipfs;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import org.zella.tuapse.model.es.FoundTorrent;

import java.util.List;

public interface IpfsSearch {

    Observable<List<FoundTorrent>> search(String text);

}
