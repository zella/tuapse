package org.zella.tuapse.webtorrent.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.reactivex.Notification;
import io.reactivex.Single;
import org.zella.tuapse.model.torrent.LiveTorrent;
import org.zella.tuapse.providers.RxUtils;

import java.util.concurrent.TimeUnit;

public class CachedWebtorrent extends DefaultWebtorrent {

    private final Cache<String, Notification<LiveTorrent>> cache = CacheBuilder.newBuilder()
            .maximumSize(128)
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();

    @Override
    public Single<LiveTorrent> webtorrent(String hash) {
        return super.webtorrent(hash).compose(RxUtils.cachedShared(cache, hash));
    }
}
