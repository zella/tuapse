package org.zella.tuapse.providers;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.*;

public class TuapseSchedulers {

    private static final int WebtorrConcurency = Integer.parseInt(System.getenv().getOrDefault("WEBTORRENT_PARALLELISM", "4"));

    private static final int forSpider = Math.max(WebtorrConcurency / 4, 1);
    private static final int forSearch = Math.max(WebtorrConcurency - forSpider, 1);

    public static Scheduler webtorrentSpider =
            Schedulers.from(new ThreadPoolExecutor(
                    forSpider,
                    forSpider,
                    2, TimeUnit.MINUTES,
                    new LinkedBlockingQueue<>()));


    public static Scheduler webtorrentSearch =
            Schedulers.from(new ThreadPoolExecutor(
                    forSearch,
                    forSearch,
                    2, TimeUnit.MINUTES,
                    new LifoBlockingDeque<>()));


}
