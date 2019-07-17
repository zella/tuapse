package org.zella.tuapse.model.messages.impl;

import org.zella.tuapse.search.SearchMode;

public class SearchAsk {

    public String searchString;
    public int pageSize;
    public SearchMode mode;

    //used in js TODO interface
    public String type = "searchAsk";

    public SearchAsk() {
    }

    public SearchAsk(String searchString, int pageSize, SearchMode mode) {
        this.searchString = searchString;
        this.pageSize = pageSize;
        this.mode = mode;
    }
}
