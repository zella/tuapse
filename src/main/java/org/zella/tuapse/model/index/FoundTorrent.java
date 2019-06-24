package org.zella.tuapse.model.index;

import org.zella.tuapse.model.torrent.StorableTorrent;
import org.zella.tuapse.model.torrent.LiveTorrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FoundTorrent<T extends StorableTorrent> {
    public T torrent;
    public List<Highlight> highlights;
    public float score;

    public FoundTorrent() {
    }

    public FoundTorrent(T torrent, List<Highlight> highlights, float score) {
        this.torrent = torrent;
        this.highlights = highlights;
        this.score = score;
    }

    public static <T extends StorableTorrent> FoundTorrent<T> create(T t, List<Highlight> highlights, float score) {
        FoundTorrent f = new FoundTorrent();
        f.torrent = t;
        f.highlights = highlights;
        f.score = score;
        return f;
    }


    public static List<FoundTorrent<LiveTorrent>> fillWithPeers(List<FoundTorrent<StorableTorrent>> found, List<LiveTorrent> withPeers) {

        Map<String, LiveTorrent> map = withPeers.stream()
                .collect(Collectors.toMap(t -> t.infoHash, t -> t, (oldValue, newValue) -> oldValue));
        List<FoundTorrent<LiveTorrent>> res = new ArrayList<>();
        for (FoundTorrent<StorableTorrent> f : found) {
            var t = map.get(f.torrent.infoHash);
            if (t != null)
                res.add(FoundTorrent.create(t, f.highlights, f.score));
        }
        return res;
    }

    @Override
    public String toString() {
        return torrent.toString();
    }
}
