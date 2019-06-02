package org.zella.tuapse.storage.impl;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.zella.tuapse.model.torrent.TFile;
import org.zella.tuapse.model.torrent.Torrent;
import org.zella.tuapse.storage.TestCase;
import org.zella.tuapse.storage.impl.EsIndex;

// for assertions on Java 8 types
import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class EsTest {

    @ClassRule
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
                    .withLocalCompose(true)
                    .waitingFor("elasticsearch",  Wait.forHttp("/all")
                            .forStatusCode(200)
                            .forStatusCode(404)
                    );


    @Test
    public void searchTest() throws InterruptedException {

        var es = new EsIndex();

        TestCase.indexCase(es);
    }
}
