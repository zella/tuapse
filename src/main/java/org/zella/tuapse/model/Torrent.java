package org.zella.tuapse.model;

import java.util.List;

public class Torrent {

    public String infoHash;
    public String name;
    public List<TFile> files;

    public Torrent() {
    }

    @Override
    public String toString() {
        return infoHash + ", " + name;
    }
}
