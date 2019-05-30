package org.zella.tuapse.es;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.zella.tuapse.model.torrent.TFile;
import org.zella.tuapse.model.torrent.Torrent;

// for assertions on Java 8 types
import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class EsTest {

    @ClassRule
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/compose-test.yml"));

    @Test
    public void searchTest() throws IOException, InterruptedException {

        Thread.sleep(30000);

        var es = new Es();
        es.createIndexIfNotExist();

        var f1 = TFile.create(0, "/music/ДДТ/осень.mp3", 1000);
        var f2 = TFile.create(1, "sdfsdfaasd fafd sfa dsf dsaasdf afsd fd sa/music/ДДТ/01.актриса весна.mp3 aa sdf fdasf dsf dsfd sfd ", 1000);
        var f3 = TFile.create(2, "/music/ДДТ/в последнюю осень.mp3", 1000);
        var t1 = Torrent.create("ДДТ", "hash1", List.of(f1, f2, f3));

        var f4 = TFile.create(0, "/music/БИ 2/01.полковник.mp3", 1000);
        var t2 = Torrent.create("БИ 2", "hash2", List.of(f4));

        var f5 = TFile.create(0, "/films/oscar/Зеленый Слоник.3gp", 1000);
        var t3 = Torrent.create("Зеленый слоник", "hash3", List.of(f5));

        var f6 = TFile.create(0, "/films/good/forest_gamp.avi", 1000);
        var t4 = Torrent.create("Gamp", "hash4", List.of(f6));

        es.insertTorrent(t1);
        es.insertTorrent(t2);
        es.insertTorrent(t3);
        es.insertTorrent(t4);

        Thread.sleep(5000);

        var search1 = es.search("ДДТ");
        assertThat(search1.size()).isEqualTo(1);
        assertThat(search1.get(0).highlights.size()).isEqualTo(3);

        var search2 = es.search("полковник");
        assertThat(search2.size()).isEqualTo(1);
        assertThat(search2.get(0).highlights.size()).isEqualTo(1);

        var search3 = es.search("films");
        assertThat(search3.size()).isEqualTo(2);
        assertThat(search3.get(0).highlights.size()).isEqualTo(1);
        assertThat(search3.get(1).highlights.size()).isEqualTo(1);

        var search4 = es.search("oscar");
        assertThat(search4.size()).isEqualTo(1);
        assertThat(search4.get(0).highlights.size()).isEqualTo(1);

        var search5 = es.search("актриса весна ддт");
        assertThat(search5.size()).isEqualTo(1);
        assertThat(search5.get(0).highlights.size()).isEqualTo(3);

        es.isSpaceAllowed();

    }
}
