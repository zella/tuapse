package org.zella.tuapse.model.messages.impl;

public class SearchAsk {

    public String searchString;
    public int pageSize;

    //used in js TODO interface
    public String type = "searchAsk";

    public SearchAsk() {
    }

    public SearchAsk(String searchString, int pageSize) {
        this.searchString = searchString;
        this.pageSize = pageSize;
    }
}
