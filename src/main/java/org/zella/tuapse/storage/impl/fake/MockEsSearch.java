package org.zella.tuapse.storage.impl.fake;

import org.zella.tuapse.model.es.FoundTorrent;
import org.zella.tuapse.model.es.Highlight;
import org.zella.tuapse.model.es.IndexMeta;
import org.zella.tuapse.model.torrent.TFile;
import org.zella.tuapse.model.torrent.Torrent;
import org.zella.tuapse.storage.impl.EsIndex;

import java.util.List;

public class MockEsSearch extends EsIndex {

    @Override
    public List<FoundTorrent> search(String what) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return List.of(FoundTorrent.create(Torrent.create("Большой длинный торрент", "EE923D0EC37ED43D689BFBAD06E845DCF8353515",
                List.of(TFile.create(0, "/ddt/osen.mp3", 999))),
                List.of(Highlight.create(1, "asd <B> asd asd </B> asd")), 9.3f),
                FoundTorrent.create(Torrent.create("Большой длинный торрент 2", "EE923D0EC37ED43D689BFBAD06E845DCF8353519",
                        List.of(TFile.create(0, "/ddt/osen.mp3", 999), TFile.create(1, "/ddt/osen2.mp3", 999999))),
                        List.of(Highlight.create(0, "asd <B> asd asd </B> asd"), Highlight.create(1, "Привет <B> от подсветки</B> а тут нет")), 9.3f));
    }

    @Override
    public void createIndexIfNotExist() {
        //do nothing
    }

    @Override
    public IndexMeta indexMeta() {
        return new IndexMeta(0, 0);
    }

    @Override
    public Boolean isSpaceAllowed() {
        return false;
    }
}
