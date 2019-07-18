package org.zella.tuapse.storage.impl;

import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.zella.tuapse.model.index.FoundTorrent;
import org.zella.tuapse.model.index.Highlight;
import org.zella.tuapse.model.index.IndexMeta;
import org.zella.tuapse.model.torrent.StorableTorrent;
import org.zella.tuapse.providers.Json;
import org.zella.tuapse.search.SearchMode;
import org.zella.tuapse.storage.AbstractIndex;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class EsIndex extends AbstractIndex {


    private static final int SearchTimeout = Integer.parseInt(System.getenv().getOrDefault("SEARCH_TIMEOUT_SEC", "60"));

    private static final int EsPort = Integer.parseInt(System.getenv().getOrDefault("ES_PORT", "9200"));
    private static final String EsHost = (System.getenv().getOrDefault("ES_HOST", "localhost"));
    private static final String EsScheme = (System.getenv().getOrDefault("ES_SCHEME", "http"));


    private final RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(
                    new HttpHost(EsHost, EsPort, EsScheme)));

    public String insertTorrent(StorableTorrent t) {
        try {
            IndexRequest indexRequest = new IndexRequest("torrents")
                    .source(Json.mapper.writeValueAsString(t), XContentType.JSON)
                    .id(t.infoHash);
            var response = client.index(indexRequest, RequestOptions.DEFAULT);

            return response.getId();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void insertTorrents(List<StorableTorrent> torrents) {
        torrents.forEach(this::insertTorrent);
    }

    public void createIndexIfNotExist() {
        try {
            GetIndexRequest req1 = new GetIndexRequest("torrents");
            if (!client.indices().exists(req1, RequestOptions.DEFAULT)) {
                logger.info("Creating index...");
                CreateIndexRequest req2 = new CreateIndexRequest("torrents");

                XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject("properties")
                        .startObject("files.path")
                        .field("analyzer", "simple")
                        .field("type", "text")
                        .endObject()
                        .endObject().endObject();

                req2.mapping(mapping);

                client.indices().create(req2, RequestOptions.DEFAULT);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    //TODO support paginations in es
    //TODO support search mode
    public List<FoundTorrent<StorableTorrent>> search(String what, SearchMode mode, int page, int pageSize) {
        try {
            SearchRequest searchRequest = Requests.searchRequest("torrents");

            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            sourceBuilder.query(QueryBuilders.boolQuery()
                    .should(QueryBuilders.matchQuery("files.path", what))
                    .should(QueryBuilders.matchQuery("name", what)));
            sourceBuilder.from(0);
            sourceBuilder.size(Math.min(pageSize, PageSize));
            sourceBuilder.timeout(new TimeValue(SearchTimeout, TimeUnit.SECONDS));

            HighlightBuilder highlightBuilder = new HighlightBuilder();
            HighlightBuilder.Field highlightTitle =
                    new HighlightBuilder.Field("files.path");
            highlightTitle.highlighterType("unified");
            highlightBuilder.field(highlightTitle);
            highlightBuilder.preTags("<B>");
            highlightBuilder.postTags("</B>");

            sourceBuilder.highlighter(highlightBuilder);

            searchRequest.source(sourceBuilder);

            var response = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            SearchHit[] searchHits = hits.getHits();

            var result = new ArrayList<FoundTorrent<StorableTorrent>>();
            logger.debug("Found:");
            for (SearchHit hit : searchHits) {
                // do something with the SearchHit
                StorableTorrent torrent = Json.mapper.readValue(hit.getSourceAsString(), StorableTorrent.class);
                logger.debug(hit.getSourceAsString());
                logger.debug("Highlights:");
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                var highlights = new ArrayList<Highlight>();
                HighlightField highlight = highlightFields.get("files.path");
                if (highlight != null) {
                    Text[] fragmentsArr = highlight.fragments();
                    var fragments = Arrays.stream(fragmentsArr).limit(highlightsLimit).collect(Collectors.toList());
                    for (Text f : fragments) {
                        var highlightString = f.string();
                        var unhtmled = highlightString.replace("<B>", "").replace("</B>", "");
                        var fileOpt = torrent.files.stream().filter(fi -> fi.path.equals(unhtmled)).findFirst();
                        if (fileOpt.isPresent())
                            highlights.add(Highlight.create(fileOpt.get().index, highlightString, fileOpt.get().length, -1));
                        else {
                            logger.warn("Highlight error for: " + highlightString);
                            highlights.add(Highlight.create(-1, highlightString, -1, -1));
                        }
                        logger.debug(highlightString);
                    }
                }
                result.add(FoundTorrent.create(torrent, highlights, hit.getScore()));
            }
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    @Override
    protected IndexMeta indexMetaInternal() throws IOException {
        var resp = client.getLowLevelClient().performRequest(new Request("GET", "torrents/_stats"));
        var body = Json.mapper.readTree(resp.getEntity().getContent());

        var size = body.get("indices").get("torrents").get("primaries").get("store").get("size_in_bytes").asLong();
        var count = body.get("indices").get("torrents").get("primaries").get("docs").get("count").asLong();

        return new IndexMeta(size, count);
    }
}
