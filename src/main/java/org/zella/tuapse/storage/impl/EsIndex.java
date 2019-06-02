package org.zella.tuapse.storage.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
import org.elasticsearch.index.mapper.ParseContext;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.model.es.FoundTorrent;
import org.zella.tuapse.model.es.IndexMeta;
import org.zella.tuapse.model.torrent.Torrent;
import org.zella.tuapse.providers.Json;
import org.zella.tuapse.storage.Index;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EsIndex implements Index {

    private static final Logger logger = LoggerFactory.getLogger(EsIndex.class);

    private static final int SearchTimeout = Integer.parseInt(System.getenv().getOrDefault("SEARCH_TIMEOUT_SEC", "60"));
    private static final int PageSize = Integer.parseInt(System.getenv().getOrDefault("PAGE_SIZE", "10"));

    private static final int EsPort = Integer.parseInt(System.getenv().getOrDefault("ES_PORT", "9200"));
    private static final String EsHost = (System.getenv().getOrDefault("ES_HOST", "localhost"));
    private static final String EsScheme = (System.getenv().getOrDefault("ES_SCHEME", "http"));
    private static final long EsMaxIndexSizeGb = Long.parseLong(System.getenv().getOrDefault("ES_MAX_INDEX_SIZE_GB", "10"));

    private static final String KEY_META = "KEY_META";


    private final LoadingCache<String, IndexMeta> indexMetaCache = CacheBuilder.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public IndexMeta load(String key) throws Exception {
                    logger.info("Request index meta...");
                    return EsIndex.this.indexMetaInternal();
                }
            });

    private final RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(
                    new HttpHost(EsHost, EsPort, EsScheme)));

    public String insertTorrent(Torrent t) {
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

    public List<FoundTorrent> search(String what) {
        try {
            SearchRequest searchRequest = Requests.searchRequest("torrents");

            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            sourceBuilder.query(QueryBuilders.boolQuery()
                    .should(QueryBuilders.matchQuery("files.path", what))
                    .should(QueryBuilders.matchQuery("name", what)));
            sourceBuilder.from(0);
            sourceBuilder.size(PageSize);
            sourceBuilder.timeout(new TimeValue(SearchTimeout, TimeUnit.SECONDS));

            HighlightBuilder highlightBuilder = new HighlightBuilder();
            HighlightBuilder.Field highlightTitle =
                    new HighlightBuilder.Field("files.path");
            highlightTitle.highlighterType("unified");
            highlightBuilder.field(highlightTitle);

            sourceBuilder.highlighter(highlightBuilder);

            searchRequest.source(sourceBuilder);

            var response = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            SearchHit[] searchHits = hits.getHits();

            var result = new ArrayList<FoundTorrent>();
            logger.debug("Found:");
            for (SearchHit hit : searchHits) {
                // do something with the SearchHit
                Torrent torrent = Json.mapper.readValue(hit.getSourceAsString(), Torrent.class);
                logger.debug(hit.getSourceAsString());
                logger.debug("Highlights:");
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                var highlights = new ArrayList<String>();
                HighlightField highlight = highlightFields.get("files.path");
                if (highlight != null) {
                    Text[] fragments = highlight.fragments();
                    for (Text f : fragments) {
                        highlights.add(f.string());
                        logger.debug(f.string());
                    }
                }
                result.add(FoundTorrent.create(torrent, highlights, hit.getScore()));
            }
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    public Boolean isSpaceAllowed() {
        var sizeGb = indexMetaCache.getUnchecked(KEY_META).indexSize / 1024d / 1024d / 1024d;
        logger.info("Index gb: " + new DecimalFormat("#.######").format(sizeGb));
        return (sizeGb < EsMaxIndexSizeGb);
    }

    public IndexMeta indexMeta() {
        return indexMetaCache.getUnchecked(KEY_META);
    }

    private IndexMeta indexMetaInternal() throws IOException {
        var resp = client.getLowLevelClient().performRequest(new Request("GET", "torrents/_stats"));
        var body = Json.mapper.readTree(resp.getEntity().getContent());

        var size = body.get("indices").get("torrents").get("primaries").get("store").get("size_in_bytes").asLong();
        var count = body.get("indices").get("torrents").get("primaries").get("docs").get("count").asLong();

        return new IndexMeta(size, count);
    }

}
