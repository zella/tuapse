package org.zella.tuapse.search;

import io.reactivex.Observable;
import org.junit.Test;
import org.zella.tuapse.importer.Importer;
import org.zella.tuapse.model.index.FoundTorrent;
import org.zella.tuapse.model.torrent.LiveTorrent;
import org.zella.tuapse.model.torrent.StorableTorrent;
import org.zella.tuapse.model.torrent.TFile;
import org.zella.tuapse.storage.Index;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;


public class SearchTest {

    @Test
    public void searchFile() throws InterruptedException {

        var index = mock(Index.class);

        when(index.search("пелагея казак")).thenReturn(List.of(
                FoundTorrent.create(StorableTorrent.create(
                        "пелагея", "h1", List.of(
                                TFile.create(0, "/Пелагея 2009/Тропы/тропы.mp3", -1),
                                TFile.create(1, "/Пелагея/Крутое/казак.mp3", -1),
                                TFile.create(2, "/Пелагея/Крутое/вишня.mp3", -1)
                        )), List.of(), -1
                ),
                FoundTorrent.create(StorableTorrent.create(
                        "пелагея", "h2", List.of(
                                TFile.create(0, "/Пелагея 2111/1/тропы.wav", -1),
                                TFile.create(1, "/Пелагея/1/демо.m4a", -1),
                                TFile.create(2, "/Пелагея/1/мамка.wma", -1)
                        )), List.of(), -1
                ))
        );

        var imported = mock(Importer.class);

        when(imported.evalTorrentsData(any(), any())).thenReturn(Observable.just(
                LiveTorrent.create("dont care", "h1", List.of(), 1),
                LiveTorrent.create("dont care", "h2", List.of(), 1)
        ));

        var source = new Search(index, imported);

        var result1 = source.searchFileEvalPeers("пелагея казак", Optional.of(Set.of("wma", "m4a", "mp3", "wav")), 1).blockingGet();

        assertThat(result1.fileWithMeta.file.path).isEqualTo("/Пелагея/Крутое/казак.mp3");

        var result2 = source.searchFileEvalPeers("пелагея казак", Optional.empty(), 1).blockingGet();

        assertThat(result2.fileWithMeta.file.path).isEqualTo("/Пелагея/Крутое/казак.mp3");
    }
}
