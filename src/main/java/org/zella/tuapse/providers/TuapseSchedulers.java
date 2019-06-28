package org.zella.tuapse.providers;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TuapseSchedulers {

    private static final int WebtorrConcurency = Integer.parseInt(System.getenv().getOrDefault("WEBTORRENT_CONCURRENCY", "4"));

    private static final int forSpider = Math.max(WebtorrConcurency / 4, 1);
    private static final int forSearch = WebtorrConcurency - forSpider;

    public static Scheduler webtorrentSpider() {
        return Schedulers.from(new ThreadPoolExecutor(
                forSpider,
                forSpider,
                2, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>()));
    }

    public static Scheduler webtorrentSearch() {
        return Schedulers.from(new ThreadPoolExecutor(
                forSearch,
                forSearch,
                2, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>()));
    }

}
