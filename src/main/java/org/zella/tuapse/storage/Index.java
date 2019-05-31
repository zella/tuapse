package org.zella.tuapse.storage;

import org.zella.tuapse.model.es.FoundTorrent;
import org.zella.tuapse.model.es.IndexMeta;
import org.zella.tuapse.model.torrent.Torrent;

import java.util.List;

public interface Index {

    String insertTorrent(Torrent t);

    void createIndexIfNotExist();

    List<FoundTorrent> search(String what);

    Boolean isSpaceAllowed();

    IndexMeta indexMeta();

}
