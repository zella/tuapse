package org.zella.tuapse.ipfs;

import io.reactivex.Observable;
import io.reactivex.Single;
import org.zella.tuapse.model.es.FoundTorrent;

import java.util.List;

public interface P2pInterface {

    Single<IpfsMeta> getPeers();

    Observable<List<FoundTorrent>> search(String text);

    class IpfsMeta {
        public final List<String> peers;

        public final int count;

        public IpfsMeta(List<String> peers) {
            this.peers = peers;
            this.count = peers.size();
        }
    }

}
