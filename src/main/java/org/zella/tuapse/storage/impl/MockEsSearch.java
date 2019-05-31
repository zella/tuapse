package org.zella.tuapse.storage.impl;

import org.zella.tuapse.model.es.FoundTorrent;
import org.zella.tuapse.model.torrent.TFile;
import org.zella.tuapse.model.torrent.Torrent;

import java.util.List;

public class MockEsSearch extends EsIndex {

    @Override
    public List<FoundTorrent> search(String what)  {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return List.of(FoundTorrent.create(Torrent.create("Большой длинный торрент", "EE923D0EC37ED43D689BFBAD06E845DCF8353519",
                List.of(TFile.create(0, "/ddt/osen.mp3", 999))),
                List.of("asd <em> asd asd </em> asd", "123123 ds;f l", "f sdf <em> dsf </em>"), 9.3f),
                FoundTorrent.create(Torrent.create("Большой длинный торрент 2", "EE923D0EC37ED43D689BFBAD06E845DCF8353519",
                        List.of(TFile.create(0, "/ddt/osen.mp3", 999),TFile.create(1, "/ddt/osen2.mp3", 999999))),
                        List.of("asd <em> asd asd </em> asd", "123123 ds;f l", "f sdf <em> dsf </em>"), 9.3f));
    }
}