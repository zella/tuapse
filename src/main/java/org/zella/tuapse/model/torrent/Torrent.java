package org.zella.tuapse.model.torrent;

import java.util.List;

public class Torrent {

    public String infoHash;
    public String name;
    public List<TFile> files;

    public Torrent() {
    }

    public static Torrent create(String name, String infoHash, List<TFile> files){
        var t = new Torrent();
        t.name = name;
        t.infoHash = infoHash;
        t.files = files;
        return t;
    }

    @Override
    public String toString() {
        return infoHash + ", " + name;
    }
}
