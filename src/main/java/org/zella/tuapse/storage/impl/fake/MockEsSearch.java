package org.zella.tuapse.storage.impl.fake;

import org.zella.tuapse.model.index.FoundTorrent;
import org.zella.tuapse.model.index.Highlight;
import org.zella.tuapse.model.index.IndexMeta;
import org.zella.tuapse.model.torrent.TFile;
import org.zella.tuapse.model.torrent.StorableTorrent;
import org.zella.tuapse.search.SearchMode;
import org.zella.tuapse.storage.impl.EsIndex;

import java.util.List;

public class MockEsSearch extends EsIndex {

    @Override
    public List<FoundTorrent<StorableTorrent>> search(String what, SearchMode mode, int page) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return List.of(FoundTorrent.create(StorableTorrent.create("Большой длинный торрент", "EE923D0EC37ED43D689BFBAD06E845DCF8353515",
                List.of(TFile.create(0, "/ddt/osen.mp3", 999))),
                List.of(Highlight.create(1, "asd <B> asd asd </B> asd", 999)), 9.3f),
                FoundTorrent.create(StorableTorrent.create("Большой длинный торрент 2", "EE923D0EC37ED43D689BFBAD06E845DCF8353519",
                        List.of(TFile.create(0, "/ddt/osen.mp3", 999), TFile.create(1, "/ddt/osen2.mp3", 999999))),
                        List.of(Highlight.create(0, "asd <B> asd asd </B> asd", 12356564123123123L), Highlight.create(1, "Привет <B> от подсветки</B> а тут нет",400000000000000L)), 9.3f));
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
