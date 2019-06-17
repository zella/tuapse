package org.zella.tuapse.model.index;

public class Highlight {

    public int index;
    public String path;
    public long length;


    public Highlight(int index, String path, long length) {
        this.index = index;
        this.path = path;
        this.length = length;
    }

    public Highlight() {
    }

    public static Highlight create(int index, String path, long length) {
        return new Highlight(index, path, length);
    }
}
