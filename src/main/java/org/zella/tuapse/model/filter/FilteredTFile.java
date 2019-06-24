package org.zella.tuapse.model.filter;

public class FilteredTFile {

    public final TFileWithMeta fileWithMeta;
    public final double score;

    public FilteredTFile(TFileWithMeta fileWithMeta, double score) {
        this.fileWithMeta = fileWithMeta;
        this.score = score;
    }
}
