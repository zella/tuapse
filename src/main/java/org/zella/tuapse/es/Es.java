package org.zella.tuapse.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Single;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.model.Torrent;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class Es {

    private static final Logger logger = LoggerFactory.getLogger(Es.class);


    private static final int EsPort = Integer.parseInt(System.getenv().getOrDefault("ES_PORT", "9200"));
    private static final String EsHost = (System.getenv().getOrDefault("ES_HOST", "localhost"));
    private static final String EsScheme = (System.getenv().getOrDefault("ES_SCHEME", "http"));

    private volatile long indexSizeBytes;

    private final RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(
                    new HttpHost(EsHost, EsPort, EsScheme)));

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
            client.indices().create(req2, RequestOptions.DEFAULT);
        }
    }

    //TODO search
    public Single<String> search(String search) {
        return Single.timer((int) (Math.random() * (10 - 1)) + 1, TimeUnit.SECONDS).map(t -> InetAddress.getLocalHost().getHostName());
    }

    //TODO check every https://stackoverflow.com/questions/55361436/how-to-cache-single-with-expiration-time
    public Boolean isSpaceAllowed() {
        return true;
    }


}
