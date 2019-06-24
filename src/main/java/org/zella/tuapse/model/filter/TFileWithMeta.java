package org.zella.tuapse.model.filter;

import org.zella.tuapse.model.torrent.TFile;

public class TFileWithMeta {

    public TFile file;
    public String hash;

    public TFileWithMeta() {
    }

    public TFileWithMeta(TFile file, String hash) {
        this.file = file;
        this.hash = hash;
    }
}
