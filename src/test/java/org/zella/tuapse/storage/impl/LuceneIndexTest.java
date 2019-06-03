package org.zella.tuapse.storage.impl;


import org.junit.Test;
import org.zella.tuapse.storage.TestCase;

import java.io.IOException;
import java.nio.file.Files;

public class LuceneIndexTest {

    @Test
    public void searchTest() throws IOException, InterruptedException {

        var dir = Files.createTempDirectory("tuapse");

        var es = new LuceneIndex(dir);

        TestCase.searchCase(es);

    }
}
