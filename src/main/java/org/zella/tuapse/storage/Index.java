package org.zella.tuapse.storage;

import org.zella.tuapse.model.index.FoundTorrent;
import org.zella.tuapse.model.index.IndexMeta;
import org.zella.tuapse.model.torrent.StorableTorrent;

import java.util.List;

public interface Index {

    String insertTorrent(StorableTorrent t);

    void createIndexIfNotExist();

    List<FoundTorrent<StorableTorrent>> search(String what);

    List<FoundTorrent<StorableTorrent>> search(String what, int pageSize);

    Boolean isSpaceAllowed();

    IndexMeta indexMeta();

}
