package org.zella.tuapse.model.index;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Highlight {

    public String path;
    public String pathFormatted;
    public long length;

    @JsonIgnore
    public float score;


    public Highlight(String path, String pathFormatted, long length, float score) {
        this.path = path;
        this.pathFormatted = pathFormatted;
        this.length = length;
        this.score = score;
    }

    public Highlight() {
    }

    public static Highlight create(String path, String pathFormatter, long length, float score) {
        return new Highlight(path, pathFormatter, length, score);
    }
}
