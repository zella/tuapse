package org.zella.tuapse.storage;

import org.zella.tuapse.model.index.FoundTorrent;
import org.zella.tuapse.model.index.IndexMeta;
import org.zella.tuapse.model.torrent.StorableTorrent;
import org.zella.tuapse.search.SearchMode;

import java.util.List;

public interface Index {

    String insertTorrent(StorableTorrent t);

    void insertTorrents(List<StorableTorrent> torrents);

    void createIndexIfNotExist();

    List<FoundTorrent<StorableTorrent>> search(String what, SearchMode mode, int page);

    List<FoundTorrent<StorableTorrent>> search(String what, SearchMode mode, int page, int pageSize);

    Boolean isSpaceAllowed();

    IndexMeta indexMeta();

}
