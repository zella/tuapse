package org.zella.tuapse.model.net;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class SingleFileSearchInput {

    public String text;
    public Optional<Set<String>> extensions;

    public SingleFileSearchInput(String text, Optional<Set<String>> extensions) {
        this.text = text;
        this.extensions = extensions;
    }

    public static SingleFileSearchInput fromRequestParams(io.vertx.reactivex.core.MultiMap params) {
        var text = params.get("text");
        var exts = new HashSet<>(params.getAll("ext"));
        Optional<Set<String>> extOpt = exts.isEmpty() ? Optional.empty() : Optional.of(exts);
        return new SingleFileSearchInput(text, extOpt);
    }
}
