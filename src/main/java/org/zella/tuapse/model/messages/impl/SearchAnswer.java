package org.zella.tuapse.model.messages.impl;

import org.zella.tuapse.model.es.FoundTorrent;

import java.util.List;

public class SearchAnswer {

    public List<FoundTorrent> torrents;

    public String type = "searchAnswer";

    public SearchAnswer() {
    }

    public static SearchAnswer create(List<FoundTorrent> t) {
        var a = new SearchAnswer();
        a.torrents = t;
        return a;
    }
}
