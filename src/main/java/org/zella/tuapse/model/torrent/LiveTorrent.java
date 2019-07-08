package org.zella.tuapse.model.torrent;

import java.util.List;

/**
 * Live info evaluated from webtorrent
 */
public class LiveTorrent extends StorableTorrent {

    public int numPeers;

    public LiveTorrent() {
    }

    public static LiveTorrent create(String name, String infoHash, List<TFile> files, int peers) {
        var t = new LiveTorrent();
        t.name = name;
        t.infoHash = infoHash;
        t.files = files;
        t.numPeers = peers;
        return t;
    }
}
