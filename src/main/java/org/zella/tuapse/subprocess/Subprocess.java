package org.zella.tuapse.subprocess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidmoten.rx2.Strings;
import com.github.zella.rxprocess2.RxNuProcessBuilder;
import com.github.zella.rxprocess2.errors.ProcessException;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.model.Torrent;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Subprocess {

    private static final Logger logger = LoggerFactory.getLogger(Subprocess.class);
    //TODO full path with "node.exe"
    private static final int WebTorrentTimeoutSec = Integer.parseInt(System.getenv().getOrDefault("WEBTORRENT_TIMEOUT_SEC", "2"));
    private static final String WebTorrentExec = (System.getenv().getOrDefault("WEBTORRENT_EXEC", "dht_web/webtorrent.js"));
    private static final String SpiderExec = (System.getenv().getOrDefault("SPIDER_EXEC", "dht_web/spider.js"));
    private static final String IpfsRoomExec = (System.getenv().getOrDefault("IPFSROOM_EXEC", "dht_web/ipfsroom.js"));

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Flowable<String> spider() {

        List<String> cmd = List.of("node", SpiderExec);

        return RxNuProcessBuilder.fromCommand(cmd)
                .asStdOut().toFlowable(BackpressureStrategy.BUFFER)
                .compose(src -> Strings.decode(src, Charset.defaultCharset()))
                .compose(src -> Strings.split(src, System.lineSeparator()))
                .filter(s -> s.startsWith("[hash]"))
                .map(s -> s.substring("[hash]".length()))
                .doOnNext(t -> logger.debug("Found torrent [" + t + "]"))
                .doOnSubscribe(d -> logger.debug("Starting spider.js"));

    }

    public static Single<Torrent> webtorrent(String hash) {

        List<String> cmd = List.of("node", WebTorrentExec, hash);
        return RxNuProcessBuilder.fromCommand(cmd)
                .asStdOutSingle(WebTorrentTimeoutSec, TimeUnit.SECONDS)
                .map(String::new)
                .map(s -> IOUtils.readLines(new StringReader(s)).stream()
                        .filter(l -> l.startsWith("[torrent]"))
                        .map(l -> l.substring("[torrent]".length())).findFirst().get())
                .map(s -> objectMapper.readValue(s, Torrent.class))
                .doOnSubscribe(d -> logger.debug("Fetch files for [" + hash + "] ..."))
                .doOnSuccess(t -> logger.debug("Fetched files for [" + hash + "]"));
    }

//    public static IpfsInterface ipfsRoom() {
//        List<String> cmd = List.of("node", IpfsRoomExec);
//        var streams = RxNuProcessBuilder.fromCommand(cmd).asStdInOut();
//        return new IpfsInterface(streams);
//    }

    public static Single<IpfsInterface> ipfsRoom() {
        List<String> cmd = List.of("node", IpfsRoomExec);
        var streams = RxNuProcessBuilder.fromCommand(cmd).asStdInOut();
        //TODO untested
        return Single.create(emitter -> {
            streams.started().subscribe((nuProcess) -> emitter.onSuccess(new IpfsInterface(streams)),
                    throwable -> {
                        if (!emitter.isDisposed())
                            emitter.onError(throwable);
                    });
            streams.waitDone().subscribe(
                    exit -> emitter.onError(exit.err.orElse(new ProcessException(-1, "Process exits without failure"))),
                    throwable -> {
                        if (!emitter.isDisposed())
                            emitter.onError(throwable);
                    });
        });


    }

}
