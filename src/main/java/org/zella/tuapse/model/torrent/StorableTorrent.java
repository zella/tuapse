package org.zella.tuapse.model.torrent;

import java.util.List;

/**
 * Record stored in index
 */
public class StorableTorrent {

    public String infoHash;
    public String name;
    public List<TFile> files;

    public StorableTorrent() {
    }

    public static StorableTorrent create(String name, String infoHash, List<TFile> files){
        var t = new StorableTorrent();
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
