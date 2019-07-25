package org.zella.tuapse.webtorrent.impl;

import com.github.zella.rxprocess2.RxNuProcessBuilder;
import io.reactivex.Single;
import org.apache.commons.io.IOUtils;
import org.zella.tuapse.model.torrent.LiveTorrent;
import org.zella.tuapse.providers.Json;
import org.zella.tuapse.webtorrent.WebTorrentInterface;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zella.tuapse.subprocess.Subprocess.WebTorrSpiderDir;
import static org.zella.tuapse.subprocess.Subprocess.WebTorrentExec;
import static org.zella.tuapse.subprocess.Subprocess.WebTorrentTimeoutSec;

public class DefaultWebtorrent implements WebTorrentInterface {

    //TODO envs

    @Override
    public Single<LiveTorrent> webtorrent(String hash) {

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
}
