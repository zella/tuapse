package org.zella.tuapse.subprocess;

import com.github.davidmoten.rx2.Strings;
import com.github.zella.rxprocess2.Exit;
import com.github.zella.rxprocess2.RxNuProcessBuilder;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Subprocess {

    private static final Logger logger = LoggerFactory.getLogger(Subprocess.class);

    private static final int WebTorrentTimeoutSec = Integer.parseInt(System.getenv().getOrDefault("WEBTORRENT_TIMEOUT_SEC", "20"));

    public static Flowable<String> spider() {

        List<String> cmd = List.of("node", "/home/dru/git/dht_web/spider.js");

        return RxNuProcessBuilder.fromCommand(cmd)
                .asStdOut().toFlowable(BackpressureStrategy.BUFFER)
                .compose(src -> Strings.decode(src, Charset.defaultCharset()))
                .compose(src -> Strings.split(src, System.lineSeparator()))
                .filter(s -> s.startsWith("[hash]"))
                .map(s -> s.substring("[hash]".length()))
                .doOnNext(t -> logger.debug("Found torrent [" + t + "]"))
                .doOnSubscribe(d -> logger.debug("Starting spider.js"));

    }

    public static Single<Exit> webtorrent(String hash) {
        List<String> cmd = List.of("node", "/home/dru/git/dht_web/webtorrent.js", hash);
        return RxNuProcessBuilder.fromCommand(cmd)
                .asWaitDone(WebTorrentTimeoutSec, TimeUnit.SECONDS)
                .doOnSubscribe(d -> logger.debug("Fetch files for [" + hash + "] ..."))
                .doOnSuccess(t -> logger.debug("Fetched files for [" + hash + "]"));

    }
//    public static Single<Torrent> webtorrent(String hash) {
//        List<String> cmd = List.of("node", "/home/dru/git/dht_web/webtorrent.js", hash);
//        return RxNuProcessBuilder.fromCommand(cmd)
//                .asStdOut().toFlowable(BackpressureStrategy.BUFFER)
//                .compose(src -> Strings.decode(src, Charset.defaultCharset()))
//                .compose(src -> Strings.split(src, System.lineSeparator()))
//                .skipWhile(s -> !s.equals("[torrent_start]"))
//                .skip(1)
//                .takeWhile(s -> !s.equals("[torrent_end"))
//                .collectInto(new StringBuffer(), StringBuffer::append)
//                .map(StringBuffer::toString)
//                .map(s -> objectMapper.readValue(s, Torrent.class))
//                .doOnSubscribe(d -> logger.debug("Fetch files for [" + hash + "] ..."))
//                .doOnSuccess(t -> logger.debug("Fetched files for [" + hash + "]"));
//
//    }

//    public static Single<Torrent> webtorrent2(String hash) {
//        List<String> cmd = List.of("node", "/home/dru/git/dht_web/webtorrent.js", hash);
//        return RxNuProcessBuilder.fromCommand(cmd)
//                .asStdOutSingle()
//                .map(String::new)
//                .map(s -> IOUtils.readLines(new StringReader(s)).stream()
//                        .filter(l -> l.startsWith("[torrent]"))
//                        .map(l -> l.substring("[torrent]".length())).findFirst().get())
//
//                .map(s -> objectMapper.readValue(s, Torrent.class))
//                .doOnSubscribe(d -> logger.debug("Fetch files for [" + hash + "] ..."))
//                .doOnSuccess(t -> logger.debug("Fetched files for [" + hash + "]"));
//    }
}
