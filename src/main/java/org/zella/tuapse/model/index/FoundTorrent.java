package org.zella.tuapse.model.index;

import org.zella.tuapse.model.torrent.StorableTorrent;
import org.zella.tuapse.model.torrent.LiveTorrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FoundTorrent {
    public StorableTorrent torrent;
    public List<Highlight> highlights;
    public float score;

    public FoundTorrent() {
    }

    public static FoundTorrent create(StorableTorrent t, List<Highlight> highlights, float score) {
        FoundTorrent f = new FoundTorrent();
        f.torrent = t;
        f.highlights = highlights;
        f.score = score;
        return f;
    }


    public static List<FoundTorrent> fillWithPeers(List<FoundTorrent> found, List<LiveTorrent> withPeers) {

        Map<String, LiveTorrent> map = withPeers.stream()
                .collect(Collectors.toMap(t -> t.infoHash, t -> t, (oldValue, newValue) -> oldValue));
        List<FoundTorrent> res = new ArrayList<>();
        for (FoundTorrent f : found) {
            var t = map.get(f.torrent.infoHash);
            if (t != null)
                res.add(FoundTorrent.create(t, f.highlights, f.score));
        }
        return res;
    }
}
