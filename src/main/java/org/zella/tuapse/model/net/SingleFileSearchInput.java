package org.zella.tuapse.model.net;

import java.util.List;
import java.util.Optional;

public class SingleFileSearchInput {

    public String text;
    public Optional<List<String>> extensions;

    public SingleFileSearchInput(String text, Optional<List<String>> extensions) {
        this.text = text;
        this.extensions = extensions;
    }

    public static SingleFileSearchInput fromRequestParams(io.vertx.reactivex.core.MultiMap params) {
        var text = params.get("text");
        var exts = params.getAll("ext");
        Optional<List<String>> extOpt = exts.isEmpty() ? Optional.empty() : Optional.of(exts);
        return new SingleFileSearchInput(text, extOpt);
    }
}
