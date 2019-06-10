package org.zella.tuapse.model.es;

import org.zella.tuapse.model.torrent.Torrent;

import java.util.List;

public class FoundTorrent {
    public Torrent torrent;
    public List<Highlight> highlights;
    public float score;

    public FoundTorrent() {
    }

    public static FoundTorrent create(Torrent t, List<Highlight> highlights, float score){
        FoundTorrent f = new FoundTorrent();
        f.torrent = t;
        f.highlights = highlights;
        f.score = score;
        return f;
    }
}
