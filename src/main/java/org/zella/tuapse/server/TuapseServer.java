package org.zella.tuapse.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.es.Es;
import org.zella.tuapse.ipfs.impl.IpfsDisabled;
import org.zella.tuapse.ipfs.IpfsSearch;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TuapseServer {

    private static final Logger logger = LoggerFactory.getLogger(TuapseServer.class);

    private static final int Port = Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "9256"));


    private AtomicReference<IpfsSearch> ipfs = new AtomicReference<>(new IpfsDisabled());
    //TODO
    private static ObjectMapper mapper = new ObjectMapper();

    private final Es es;

    public TuapseServer(Es es) {
        this.es = es;
    }

    public void ipfsUpdate(IpfsSearch search) {
        ipfs.set(search);
    }

    public Single<HttpServer> listen() {

        Router router = Router.router(Vertx.vertx());
        router.get("/healthcheck").handler(ctx -> ctx.response().end("ok"));
        router.get("/api/v1/search").handler(ctx -> {
            //chunked response
            ctx.response().setChunked(true);
            Single.fromCallable(() -> ctx.queryParams().get("text"))
                    .flatMapObservable(text -> Observable.merge(List.of(
                            Single.fromCallable(() -> es.search(text)).toObservable(),
                            ipfs.get().search(text)
                            .doOnTerminate(() -> logger.debug("TERMINATED"))
                            )
                    )).timeout(60, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())//home usage, schedulers io will ok
                    .doOnSubscribe(d -> logger.info("Server started at " + Port + " port"))
                    .subscribe(serach -> ctx.response().write(mapper.writeValueAsString(serach) + System.lineSeparator()),
                            e -> {
                                logger.error("Error", e);
                                ctx.fail(e);
                            },
                            () -> {
                                ctx.response().end();
                            });


        });
        return Vertx.vertx().createHttpServer().
                requestHandler(router).rxListen(Port);
    }

}
