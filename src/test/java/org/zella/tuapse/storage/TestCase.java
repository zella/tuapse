package org.zella.tuapse.storage;

import org.zella.tuapse.model.torrent.TFile;
import org.zella.tuapse.model.torrent.StorableTorrent;
import org.zella.tuapse.search.SearchMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.truth.Truth.assertThat;

public class TestCase {


    public static void searchCase(Index es) throws InterruptedException {

        es.createIndexIfNotExist();

        var f1 = TFile.create("/music/ДДТ/осень.mp3", 1000);
        var f2 = TFile.create("sdfsdfaasd fafd sfa dsf dsaasdf afsd fd sa/music/ДДТ/01.актриса весна.mp3 aa sdf fdasf dsf dsfd sfd", 1000);
        var f3 = TFile.create("/music/ДДТ/в последнюю осень.mp3", 1000);
        var f3a = TFile.create("NO DDT) HERE", 1000);
        var t1 = StorableTorrent.create("ДДТ дискография", "hash1", List.of(f1, f2, f3, f3a));

        var f4 = TFile.create("/music/БИ 2/01.полковник.mp3", 1000);
        var t2 = StorableTorrent.create("БИ 2", "hash2", List.of(f4));

        var f5 = TFile.create("/films/oscar/Зеленый Слоник.3gp", 1000);
        var t3 = StorableTorrent.create("Зеленый слоник", "hash3", List.of(f5));

        var f6 = TFile.create("/films/good/forest_gamp.avi", 1000);
        var t4 = StorableTorrent.create("Gamp", "hash4", List.of(f6));

        var f7 = TFile.create("SOmeFile.lol", 1000);
        var t5 = StorableTorrent.create("Хроники: терминатор, возвращение легенды", "hash5", List.of(f7));

        es.insertTorrent(t1);
        es.insertTorrent(t2);
        es.insertTorrent(t3);
        es.insertTorrent(t4);
        es.insertTorrent(t5);

        Thread.sleep(3000);

        //FILES_AND_NAMES
        var search1 = es.search("ДДТ", SearchMode.FILES_AND_NAMES, 0);
        assertThat(search1.size()).isEqualTo(1);
        assertThat(search1.get(0).highlights.size()).isEqualTo(3);

        //test highlight
        assertThat(search1.get(0).highlights.stream().filter(h -> h.pathFormatted.equals("/music/<B>ДДТ</B>/в последнюю осень.mp3")).findFirst()).isNotEqualTo(Optional.empty());
        assertThat(search1.get(0).highlights.stream().filter(h -> h.path.equals("/music/ДДТ/в последнюю осень.mp3")).findFirst()).isNotEqualTo(Optional.empty());
        assertThat(search1.get(0).highlights.stream().filter(h -> h.pathFormatted.equals("/music/<B>ДДТ</B>/осень.mp3")).findFirst()).isNotEqualTo(Optional.empty());
        assertThat(search1.get(0).highlights.stream()
                .filter(h -> h.pathFormatted.equals("sdfsdfaasd fafd sfa dsf dsaasdf afsd fd sa/music/<B>ДДТ</B>/01.актриса весна.mp3 aa sdf fdasf dsf dsfd sfd")).findFirst()).isNotEqualTo(Optional.empty());

        var search2 = es.search("полковник", SearchMode.FILES_AND_NAMES, 0);
        assertThat(search2.size()).isEqualTo(1);
        assertThat(search2.get(0).highlights.size()).isEqualTo(1);

        var search3 = es.search("films", SearchMode.FILES_AND_NAMES, 0);
        assertThat(search3.size()).isEqualTo(2);
        assertThat(search3.get(0).highlights.size()).isEqualTo(1);
        assertThat(search3.get(1).highlights.size()).isEqualTo(1);

        var search4 = es.search("oscar", SearchMode.FILES_AND_NAMES, 0);
        assertThat(search4.size()).isEqualTo(1);
        assertThat(search4.get(0).highlights.size()).isEqualTo(1);

        var search5 = es.search("актриса весна ддт", SearchMode.FILES_AND_NAMES, 0);
        assertThat(search5.size()).isEqualTo(1);
        assertThat(search5.get(0).highlights.size()).isEqualTo(3);

        var search6 = es.search("терминатор", SearchMode.FILES_AND_NAMES, 0);
        assertThat(search6.size()).isEqualTo(1);
        assertThat(search6.get(0).highlights).isEmpty();

        //FILES
        var search7 = es.search("somefile", SearchMode.FILES, 0);
        assertThat(search7.size()).isEqualTo(1);
        assertThat(search7.get(0).highlights.size()).isEqualTo(1);

        //NAMES
        var search8 = es.search("терминатор", SearchMode.NAMES, 0);
        assertThat(search8.size()).isEqualTo(1);
        assertThat(search8.get(0).highlights).isEmpty();


        //space allowed
        assertThat(es.isSpaceAllowed()).isTrue();
    }

    public static void searchPaginationCase(Index es) throws InterruptedException {

        es.createIndexIfNotExist();

        var t1 = StorableTorrent.create("ДДТ", "hash1", List.of());
        var t2 = StorableTorrent.create("ДДТ", "hash2", List.of());
        var t3 = StorableTorrent.create("ДДТ", "hash3", List.of());
        var t4 = StorableTorrent.create("ДДТ", "hash4", List.of());
        var t5 = StorableTorrent.create("ДДТ", "hash5", List.of());
        var t6 = StorableTorrent.create("ЗЕмляне", "hash6", List.of());

        es.insertTorrents(List.of(t1, t2, t3, t4, t5, t6));

        var res = es.search("ддт", SearchMode.FILES_AND_NAMES, 1, 2);

        assertThat(res).hasSize(2);
        assertThat(res.stream().anyMatch(t -> t.torrent.infoHash.equals("hash1"))).isTrue();
        assertThat(res.stream().anyMatch(t -> t.torrent.infoHash.equals("hash2"))).isTrue();

        var res2 = es.search("ддт", SearchMode.FILES_AND_NAMES, 2, 2);

        assertThat(res2).hasSize(2);
        assertThat(res2.stream().anyMatch(t -> t.torrent.infoHash.equals("hash3"))).isTrue();
        assertThat(res2.stream().anyMatch(t -> t.torrent.infoHash.equals("hash4"))).isTrue();

    }

    public static void searchHighlightSortingCase(Index es) throws InterruptedException {

        es.createIndexIfNotExist();

        var f1 = TFile.create("/music/ДДТ/осень.mp3", 1);
        var f2 = TFile.create("/music/ДДТ/актриса весна/осень.mp3", 2);
        var f3 = TFile.create("/music/ДДТ/актриса весна/в последнюю осень.mp3", 3);
        var f4 = TFile.create("/music/ДДТ/всякое/попса.mp3", 4);


        var t1 = StorableTorrent.create("ДДТ", "hash1", List.of(
                f1, f2, f3, f4
        ));

        es.insertTorrents(List.of(t1));

        var res = es.search("ддт актриса весна в последнюю осень", SearchMode.FILES, 1, 999999);
        assertThat(res.get(0).highlights.get(0).length).isEqualTo(3);
        assertThat(res.get(0).highlights.get(1).length).isEqualTo(2);
        assertThat(res.get(0).highlights.get(2).length).isEqualTo(1);
        assertThat(res.get(0).highlights.get(3).length).isEqualTo(4);
    }


    private static StorableTorrent random() {

        var filesCount = ThreadLocalRandom.current().nextInt(199, 200);
        var files = new ArrayList<TFile>();

        for (int i = 0; i < filesCount; i++) {
            files.add(TFile.create(UUID.randomUUID().toString() + ", " + UUID.randomUUID().toString() + ',' + UUID.randomUUID().toString(),
                    1000));
        }

        return StorableTorrent.create(UUID.randomUUID().toString(), UUID.randomUUID().toString(), files);
    }

    public static void spaceAllowedCase(Index es, int n) {
        es.createIndexIfNotExist();

        for (int i = 0; i < n; i++) {
            es.insertTorrent(random());
            if (i % 10 == 0)
                System.out.println("inserted " + i);
        }
        assertThat(es.isSpaceAllowed()).isTrue();
    }

}
