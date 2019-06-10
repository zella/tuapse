package org.zella.tuapse.model.es;

public class Highlight {

    public int index;
    public String path;


    public Highlight(int index, String path) {
        this.index = index;
        this.path = path;
    }

    public Highlight() {
    }

    public static Highlight create(int index, String path) {
        return new Highlight(index, path);
    }
}
