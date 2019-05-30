package org.zella.tuapse.es;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.model.es.FoundTorrent;
import org.zella.tuapse.model.torrent.Torrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Es {

    private static final Logger logger = LoggerFactory.getLogger(Es.class);


    private static final int EsPort = Integer.parseInt(System.getenv().getOrDefault("ES_PORT", "9200"));
    private static final String EsHost = (System.getenv().getOrDefault("ES_HOST", "localhost"));
    private static final String EsScheme = (System.getenv().getOrDefault("ES_SCHEME", "http"));
    private static final long EsMaxIndexSizeGb = Long.parseLong(System.getenv().getOrDefault("ES_MAX_INDEX_SIZE_GB", "10"));

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final LoadingCache<String, Long> indexSizeCache = CacheBuilder.newBuilder()
            .maximumSize(1)
            //TODO env
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public Long load(String key) throws Exception {
                    return Es.this.indexSize();
                }
            });

    private final RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(
                    new HttpHost(EsHost, EsPort, EsScheme)));

    public String insertTorrent(Torrent t) throws IOException {
        IndexRequest indexRequest = new IndexRequest("torrents")
                .source(objectMapper.writeValueAsString(t), XContentType.JSON)
                .id(t.infoHash);
        var response = client.index(indexRequest, RequestOptions.DEFAULT);

        return response.getId();
    }

    public void createIndexIfNotExist() throws IOException {
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
    }

//    public Single<String> search(String search) {
//        return Single.timer((int) (Math.random() * (10 - 1)) + 1, TimeUnit.SECONDS).map(t -> InetAddress.getLocalHost().getHostName());
//    }

    public List<FoundTorrent> search(String what) throws IOException {

        SearchRequest searchRequest = Requests.searchRequest("torrents");

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        sourceBuilder.query(QueryBuilders.boolQuery()
                .should(QueryBuilders.matchQuery("files.path", what))
                .should(QueryBuilders.matchQuery("name", what)));
        sourceBuilder.from(0);
        //TODO env
        sourceBuilder.size(10);
        //TODO env
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

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
            Torrent torrent = objectMapper.readValue(hit.getSourceAsString(), Torrent.class);
            logger.debug(hit.getSourceAsString());
            logger.debug("Highlights:");
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField highlight = highlightFields.get("files.path");
            Text[] fragments = highlight.fragments();
            var highlights = new ArrayList<String>();
            for (Text f : fragments) {
                highlights.add(f.string());
                logger.debug(f.string());
            }
            result.add(FoundTorrent.create(torrent, highlights, hit.getScore()));
        }
        return result;
    }



    public Boolean isSpaceAllowed() {
        var sizeGb = indexSizeCache.getUnchecked("INDEX_SIZE") / 1024d / 1024d / 1024d;
        logger.info("Index gb: " + sizeGb);
        return (sizeGb > EsMaxIndexSizeGb);
    }

    private long indexSize() throws IOException {
        var resp = client.getLowLevelClient().performRequest(new Request("GET", "torrents/_stats"));
        var body = new ObjectMapper().readTree(resp.getEntity().getContent());
        return body.get("indices").get("torrents").get("primaries").get("store").get("size_in_bytes").asLong();
    }
}
