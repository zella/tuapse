package org.zella.tuapse.subprocess;

import com.github.davidmoten.rx2.Strings;
import com.github.zella.rxprocess2.RxNuProcessBuilder;
import io.reactivex.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.ipfs.impl.IpfsInterface;
import org.zella.tuapse.model.torrent.LiveTorrent;
import org.zella.tuapse.providers.Json;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Subprocess {

    private static final Logger logger = LoggerFactory.getLogger(Subprocess.class);
    //TODO full path with "node.exe"
    private static final int WebTorrentTimeoutSec = Integer.parseInt(System.getenv().getOrDefault("WEBTORRENT_TIMEOUT_SEC", "10"));
    private static final String WebTorrentExec = (System.getenv().getOrDefault("WEBTORRENT_EXEC", "dht_web/webtorrent.js"));
    private static final String GenTorrentExec = (System.getenv().getOrDefault("GENTORRENT_EXEC", "dht_web/gen-torrent-file.js"));
    private static final int GenTorrentTimeoutSec = Integer.parseInt(System.getenv().getOrDefault("GENTORRENT_TIMEOUT_SEC", "25"));
    private static final String SpiderExec = (System.getenv().getOrDefault("SPIDER_EXEC", "dht_web/spider.js"));
    private static final String IpfsRoomExec = (System.getenv().getOrDefault("IPFSROOM_EXEC", "dht_web/ipfsroom.js"));
    public static final String WebTorrGenDir = (System.getenv().getOrDefault("WEBTOR_GEN_DIR", "/tmp/webtorrent_gen/"));
    public static final String WebTorrSpiderDir = (System.getenv().getOrDefault("WEBTOR_SPIDER_DIR", "/tmp/webtorrent_spider/"));
    private static final int SpiderJumpTimeSec;

    static {
        SpiderJumpTimeSec = Integer.parseInt(System.getenv().getOrDefault("SPIDER_JUMP_SEC", "6"));
        assert SpiderJumpTimeSec > 2;
    }

    public static Flowable<String> spider() {

        List<String> cmd = List.of("node", SpiderExec);

        return RxNuProcessBuilder.fromCommand(cmd)
                .withEnv(Map.of("JUMP_NODE_SEC", String.valueOf(SpiderJumpTimeSec)))
                .asStdOut().toFlowable(BackpressureStrategy.BUFFER)
                .compose(src -> Strings.decode(src, Charset.defaultCharset()))
                .compose(src -> Strings.split(src, System.lineSeparator()))
                .filter(s -> s.startsWith("[hash]"))
                .map(s -> s.substring("[hash]".length()))
                .doOnNext(t -> logger.info("Found torrent [" + t + "]"))
                .doOnSubscribe(d -> logger.info("Starting spider.js"));

    }

    public static Single<LiveTorrent> webtorrent(String hash) {

        List<String> cmd = List.of("node", WebTorrentExec, hash);
        return RxNuProcessBuilder.fromCommand(cmd)
                .withEnv(Map.of("WEBTOR_SPIDER_DIR", WebTorrSpiderDir))
                .asStdOutSingle(WebTorrentTimeoutSec, TimeUnit.SECONDS)
                .map(String::new)
                .map(s -> IOUtils.readLines(new StringReader(s)).stream()
                        .filter(l -> l.startsWith("[torrent]"))
                        .map(l -> l.substring("[torrent]".length())).findFirst().get())
                .map(s -> Json.mapper.readValue(s, LiveTorrent.class));
    }

    public static Single<String> generateTorrentFile(String hash) {

        List<String> cmd = List.of("node", GenTorrentExec, hash);
        return RxNuProcessBuilder.fromCommand(cmd)
                .withEnv(Map.of("WEBTOR_GEN_DIR", WebTorrGenDir))
                .asStdOutSingle(GenTorrentTimeoutSec, TimeUnit.SECONDS)
                .map(String::new)
                //TODO fix me, unsafe, just use eol in js and here
                .map(l -> l.substring("[torrentFile]".length()));
    }

    public static IpfsInterface ipfsRoom() {
        logger.debug("Ipfs requested");
        List<String> cmd = List.of("node", IpfsRoomExec);
        var streams = RxNuProcessBuilder.fromCommand(cmd).asStdInOut();
        return new IpfsInterface(streams);
    }
}
