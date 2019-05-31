package org.zella.tuapse.server;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.es.Es;
import org.zella.tuapse.ipfs.IpfsSearch;
import org.zella.tuapse.ipfs.impl.IpfsDisabled;
import org.zella.tuapse.providers.Json;
import org.zella.tuapse.subprocess.Subprocess;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TuapseServer {

    private static final Logger logger = LoggerFactory.getLogger(TuapseServer.class);

    private static final int Port = Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "9257"));
    private static final int ReqTimeout = Integer.parseInt(System.getenv().getOrDefault("REQUEST_TIMEOUT_SEC", "60"));

    private AtomicReference<IpfsSearch> ipfs = new AtomicReference<>(new IpfsDisabled());

    private final Es es;

    public TuapseServer(Es es) {
        this.es = es;
    }

    public void ipfsUpdate(IpfsSearch search) {
        ipfs.set(search);
    }

    public Single<HttpServer> listen() {
        var vertx = Vertx.vertx();

        Router router = Router.router(vertx);
        router.get().handler(StaticHandler.create());
        router.get("/").handler(ctx -> ctx.reroute("/index.html"));
        router.get("/healthcheck").handler(ctx -> ctx.response().end("ok"));
        router.get("/api/v1/generateTorrentFile").handler(ctx -> {
            Single.fromCallable(() -> ctx.queryParams().get("hash"))
                    .flatMap(hash -> Subprocess.generateTorrentFile(hash).subscribeOn(Schedulers.io()))
                    .subscribe(buffer -> ctx.response().end(Buffer.buffer(Base64.decodeBase64(buffer)))
                            , e -> {
                                logger.error("Error", e);
                                ctx.fail(e);
                            });
        });
        router.get("/api/v1/search").handler(ctx -> {
            //chunked response
            ctx.response().setChunked(true);
            Single.fromCallable(() -> ctx.queryParams().get("text"))
                    .flatMapObservable(text -> Observable.merge(List.of(
                            Single.fromCallable(() -> es.search(text)).toObservable(),
                            ipfs.get().search(text))
                    )).timeout(ReqTimeout, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())//home usage, schedulers io will ok
                    .subscribe(search -> ctx.response().write(Json.mapper.writeValueAsString(search) + System.lineSeparator()),
                            e -> {
                                logger.error("Error", e);
                                ctx.response().end();
                            },
                            () -> ctx.response().end());


        });
        return Vertx.vertx().createHttpServer().
                requestHandler(router).rxListen(Port)
                .doOnSubscribe(d -> logger.info("Server started at " + Port + " port"));
    }

}
