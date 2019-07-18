package org.zella.tuapse.model.index;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Highlight {

    public int index;
    public String path;

    public long length;
    @JsonIgnore
    public float score;


    public Highlight(int index, String path, long length, float score) {
        this.index = index;
        this.path = path;
        this.length = length;
        this.score = score;
    }

    public Highlight() {
    }

    public static Highlight create(int index, String path, long length, float score) {
        return new Highlight(index, path, length, score);
    }
}
