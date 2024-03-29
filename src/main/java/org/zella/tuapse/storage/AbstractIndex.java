package org.zella.tuapse.storage;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zella.tuapse.model.index.FoundTorrent;
import org.zella.tuapse.model.index.IndexMeta;
import org.zella.tuapse.model.torrent.StorableTorrent;
import org.zella.tuapse.search.SearchMode;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class AbstractIndex implements Index {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final int PageSize = Integer.parseInt(System.getenv().getOrDefault("PAGE_SIZE", "20"));
    protected static final long MaxIndexSizeGb = Long.parseLong(System.getenv().getOrDefault("MAX_INDEX_SIZE_GB", "10"));
    protected static final long MetaCacheExpireMin = Long.parseLong(System.getenv().getOrDefault("META_CACHE_EXPIRE_MIN", "3"));

    private static final String KEY_META = "KEY_META";

    protected final int highlightsLimit = 4;

    private final LoadingCache<String, IndexMeta> indexMetaCache = CacheBuilder.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(MetaCacheExpireMin, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public IndexMeta load(String key) throws Exception {
                    logger.info("Request index meta...");
                    return AbstractIndex.this.indexMetaInternal();
                }
            });

    @Override
    public Boolean isSpaceAllowed() {
        var sizeGb = indexMetaCache.getUnchecked(KEY_META).indexSize / 1024d / 1024d / 1024d;
        logger.info("Index gb: " + new DecimalFormat("#.######").format(sizeGb));
        return (sizeGb < MaxIndexSizeGb);
    }

    @Override
    public List<FoundTorrent<StorableTorrent>> search(String what, SearchMode mode, int page) {
        return search(what, mode, page, PageSize);
    }

    @Override
    public IndexMeta indexMeta() {
        return indexMetaCache.getUnchecked(KEY_META);
    }

    protected abstract IndexMeta indexMetaInternal() throws IOException;
}
