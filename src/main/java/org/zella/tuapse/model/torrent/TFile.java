package org.zella.tuapse.model.torrent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TFile {

    public String path;
    public long length;

    public TFile() {
    }

    public static TFile create(String path, long length) {
        var t = new TFile();
        t.path = path;
        t.length = length;
        return t;
    }
}
