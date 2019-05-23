package org.zella.tuapse.model.torrent;

public class TFile {

    public int index;
    public String path;
    public long length;

    public TFile() {
    }

    public static TFile create(int index, String path, long length) {
        var t = new TFile();
        t.index = index;
        t.path = path;
        t.length = length;
        return t;
    }
}
