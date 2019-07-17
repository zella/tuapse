package org.zella.tuapse.model.net;

import org.zella.tuapse.search.SearchMode;

public class SearchInput {

    public String text;
    public int page;
    public SearchMode mode;

    public SearchInput(String text, int page, SearchMode mode) {
        this.text = text;
        this.page = page;
        this.mode = mode;
    }

    public static SearchInput fromRequestParams(io.vertx.reactivex.core.MultiMap params) {
        var text = params.get("text");
        var page = Integer.parseInt(params.get("page"));
        var mode = SearchMode.valueOf(params.get("mode"));
        return new SearchInput(text, page, mode);
    }
}
