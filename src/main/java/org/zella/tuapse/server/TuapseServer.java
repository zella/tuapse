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

public class TuapseServer {

    private static final Logger logger = LoggerFactory.getLogger(TuapseServer.class);

    private static final int Port = Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "9256"));

    private final Subject<IpfsSearch> ipfs = BehaviorSubject.<IpfsSearch>createDefault(new IpfsDisabled()).toSerialized();

    private final Single<IpfsSearch> ipfsSingle = ipfs.singleOrError();
    //TODO
    private static ObjectMapper mapper = new ObjectMapper();

    private final Es es;

    public TuapseServer(Es es) {
        this.es = es;
    }

    public Subject<IpfsSearch> ipfsUpdate() {
        return ipfs;
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
                            ipfsSingle.flatMapObservable(_ipfs -> _ipfs.search(text))
                    ))).timeout(60, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.computation())//TODO home usage, schedulers io will ok
                    .subscribe(serach -> mapper.writeValueAsString(serach + System.lineSeparator()),
                            ctx::fail,
                            () -> ctx.response().end());


        });
        return Vertx.vertx().createHttpServer().
                requestHandler(router).rxListen(Port);
    }

}
