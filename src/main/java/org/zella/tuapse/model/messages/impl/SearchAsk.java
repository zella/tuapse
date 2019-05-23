package org.zella.tuapse.model.messages.impl;

public class SearchAsk {

    public String searchString;

    public String type = "searchAsk";

    public SearchAsk() {
    }

    public SearchAsk(String searchString ) {
        this.searchString = searchString;
    }
}
