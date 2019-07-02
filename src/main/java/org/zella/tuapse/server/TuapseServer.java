package org.zella.tuapse.server;

import com.fasterxml.jackson.core.type.TypeReference;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.importer.Importer;
import org.zella.tuapse.model.net.SingleFileSearchInput;
import org.zella.tuapse.model.torrent.StorableTorrent;
import org.zella.tuapse.providers.Json;
import org.zella.tuapse.providers.TuapseSchedulers;
import org.zella.tuapse.search.Search;
import org.zella.tuapse.subprocess.Subprocess;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TuapseServer {

    private static final Logger logger = LoggerFactory.getLogger(TuapseServer.class);

    private static final int Port = Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "9257"));
    private static final int ReqTimeout = Integer.parseInt(System.getenv().getOrDefault("REQUEST_TIMEOUT_SEC", "120"));
    private static final String TuapsePlayOrigin = (System.getenv().getOrDefault("TUAPSE_PLAY_ORIGIN", "N/A"));

    private final Importer importer;
    private final Search search;

    public TuapseServer(Importer importer, Search search) {
        this.importer = importer;
        this.search = search;
    }


    public Single<HttpServer> listen() {
        var vertx = Vertx.vertx();

        Router router = Router.router(vertx);

        router.post().handler(BodyHandler.create());

        router.get("/").handler(ctx -> {
            //TODO ui different non java project
            Single.fromCallable(() -> IOUtils.toString(this.getClass().getResourceAsStream("/templates/index.template"),
                    "UTF-8"))
                    .map(template -> template.replace("[TUAPSE_PLAY_ORIGIN]", TuapsePlayOrigin))
                    .subscribeOn(Schedulers.io())
                    .subscribe(res -> ctx.response().end(res),
                            e -> {
                                logger.error("Error", e);
                                ctx.fail(e);
                            });
        });
        router.get("/healthcheck").handler(ctx -> ctx.response().end("ok"));
        router.post("/api/v1/import").handler(ctx -> readBody(ctx, new TypeReference<List<StorableTorrent>>() {
        }).doOnSuccess(h -> logger.debug(h.toString()))
                .flatMap(torrents -> importer.importTorrents(torrents).subscribeOn(Schedulers.io()))
                .subscribe(im -> ctx.response().end(Json.mapper.writeValueAsString(im)), e -> {
                    logger.error("Error", e);
                    ctx.fail(e);
                }));
        router.post("/api/v1/evalTorrents").handler(ctx -> readBody(ctx, new TypeReference<List<String>>() {
        }).doOnSuccess(h -> logger.debug(h.toString()))
                .flatMap(hashes -> importer.evalTorrentsData(hashes, TuapseSchedulers.webtorrentSearch()).toList().subscribeOn(Schedulers.io()))
                .subscribe(im -> ctx.response().end(Json.mapper.writeValueAsString(im)), e -> {
                    logger.error("Error", e);
                    ctx.fail(e);
                }));
        router.get("/api/v1/p2pMeta").handler(ctx -> search.getIpfs().get().getPeers()
                .subscribe(meta -> ctx.response().end(Json.mapper.writeValueAsString(meta)), e -> {
                    logger.error("Error", e);
                    ctx.fail(e);
                }));
        router.get("/api/v1/indexMeta").handler(ctx -> Single.fromCallable(() -> Json.mapper.writeValueAsString(search.getIndex().indexMeta()))
                .subscribe(s -> ctx.response().end(s), e -> {
                    logger.error("Error", e);
                    ctx.fail(e);
                }));
        router.get("/api/v1/generateTorrentFile").handler(ctx -> {
            Single.fromCallable(() -> ctx.queryParams().get("hash"))
                    .flatMap(hash -> Subprocess.generateTorrentFile(hash).subscribeOn(Schedulers.io()))
                    .subscribe(buffer -> ctx.response().end(Buffer.buffer(Base64.decodeBase64(buffer)))
                            , e -> {
                                logger.error("Error", e);
                                ctx.fail(e);
                            });
        });
        router.get("/api/v1/search_no_eval_peers").handler(ctx -> {
            ctx.response().setChunked(true);
            Single.fromCallable(() -> ctx.queryParams().get("text"))
                    .flatMapObservable(search::searchNoEvalPeers)
                    .timeout(ReqTimeout, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())//home usage, schedulers io will ok
                    .subscribe(search -> ctx.response().write(Json.mapper.writeValueAsString(search) + System.lineSeparator()),
                            e -> {
                                logger.error("Error", e);
                                ctx.response().end();
                            },
                            () -> ctx.response().end());
        });
        router.get("/api/v1/search_eval_peers").handler(ctx -> {
            ctx.response().setChunked(true);
            Single.fromCallable(() -> ctx.queryParams().get("text"))
                    .flatMapObservable(text -> search.searchEvalPeers(text, 4))
                    .timeout(ReqTimeout, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())//home usage, schedulers io will ok
                    .doOnNext(r -> logger.debug("Search result with peers: " + r.stream().map(t -> t.torrent.name).collect(Collectors.joining("|"))))
                    .subscribe(search -> ctx.response().write(Json.mapper.writeValueAsString(search) + System.lineSeparator()),
                            e -> {
                                logger.error("Error", e);
                                ctx.response().end();
                            },
                            () -> ctx.response().end());

        });
        router.get("/api/v1/search_file").handler(ctx -> {
            Single.fromCallable(() -> SingleFileSearchInput.fromRequestParams(ctx.request().params()))
                    .flatMap(p -> search.searchSingle(p.text, p.extensions))
                    .timeout(ReqTimeout, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())//home usage, schedulers io will ok
                    .doOnSuccess(r -> logger.debug("Found file: " + r.fileWithMeta.file.path))
                    .subscribe(r -> ctx.response().end(Json.mapper.writeValueAsString(r.fileWithMeta)), e -> {
                                logger.error("Error", e);
                                ctx.fail(e);
                            }
                    );

        });


        return vertx.createHttpServer().
                requestHandler(router).rxListen(Port);
    }

    private <T> Single<T> readBody(RoutingContext body, Class<T> valueType) {
        return Single.fromCallable(() -> Json.mapper.readValue(body.getBodyAsString(), valueType));
    }

    private <T> Single<T> readBody(RoutingContext body, TypeReference<T> type) {
        return Single.fromCallable(() -> Json.mapper.readValue(body.getBodyAsString(), type));
    }

}
