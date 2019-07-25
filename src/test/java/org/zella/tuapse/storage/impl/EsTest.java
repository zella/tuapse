package org.zella.tuapse.storage.impl;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.zella.tuapse.storage.TestCase;

// for assertions on Java 8 types
import static com.google.common.truth.Truth.assertThat;

import java.io.File;

@Ignore
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
    @Ignore
    public void searchTest() throws InterruptedException {

        var es = new EsIndex();

        TestCase.searchCase(es);
    }
}
