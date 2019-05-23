package org.zella.tuapse.model.messages.impl;

public class SearchAnswer {

    public String searchResult;

    public String type = "searchAnswer";

    public SearchAnswer() {
    }

    public SearchAnswer(String searchResult) {
        this.searchResult = searchResult;
    }
}
