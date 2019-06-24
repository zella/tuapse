package org.zella.tuapse.model.messages.impl;

import org.zella.tuapse.model.index.FoundTorrent;
import org.zella.tuapse.model.torrent.StorableTorrent;

import java.util.List;

public class SearchAnswer {

    public List<FoundTorrent<StorableTorrent>> torrents;

    public String type = "searchAnswer";

    public SearchAnswer() {
    }

    public static SearchAnswer create(List<FoundTorrent<StorableTorrent>> t) {
        var a = new SearchAnswer();
        a.torrents = t;
        return a;
    }
}
