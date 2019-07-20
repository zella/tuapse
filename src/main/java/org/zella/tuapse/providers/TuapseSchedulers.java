package org.zella.tuapse.providers;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TuapseSchedulers {

    private static final int WebtorrConcurency = Integer.parseInt(System.getenv().getOrDefault("WEBTORRENT_CONCURRENCY", "8"));

    private static final int forSpider = Math.max(WebtorrConcurency / 4, 1);
    private static final int forSearch = WebtorrConcurency - forSpider;

    public static Scheduler webtorrentSpider =
            Schedulers.from(new ThreadPoolExecutor(
                    1,
                    1,
                    2, TimeUnit.MINUTES,
                    new LinkedBlockingQueue<>()));


    public static Scheduler webtorrentSearch =
            Schedulers.from(new ThreadPoolExecutor(
                    1,
                    1,
                    2, TimeUnit.MINUTES,
                    new LinkedBlockingQueue<>()));


}
