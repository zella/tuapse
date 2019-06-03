package org.zella.tuapse.storage;

import org.zella.tuapse.model.es.FoundTorrent;
import org.zella.tuapse.model.es.IndexMeta;
import org.zella.tuapse.model.torrent.Torrent;

import java.util.List;

public interface Index {

    int PageSize = Integer.parseInt(System.getenv().getOrDefault("PAGE_SIZE", "10"));
    long MaxIndexSizeGb = Long.parseLong(System.getenv().getOrDefault("MAX_INDEX_SIZE_GB", "10"));

    String insertTorrent(Torrent t);

    void createIndexIfNotExist();

    List<FoundTorrent> search(String what);

    Boolean isSpaceAllowed();

    IndexMeta indexMeta();

}
