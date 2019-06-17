package org.zella.tuapse.model.index;

public class IndexMeta {

    public final long indexSize;
    public final long docsCount;

    public IndexMeta(long indexSize, long docsCount) {
        this.indexSize = indexSize;
        this.docsCount = docsCount;
    }
}
