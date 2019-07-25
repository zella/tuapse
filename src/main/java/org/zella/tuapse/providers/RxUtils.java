package org.zella.tuapse.providers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.reactivex.Notification;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.functions.Function;
import org.zella.tuapse.model.torrent.LiveTorrent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RxUtils {

    static class Distinct<T, R> {
        final HashSet<R> previous;
        final List<T> current;

        public Distinct(HashSet<R> previous, List<T> current) {
            this.previous = previous;
            this.current = current;
        }
    }

    public static <T, R> ObservableTransformer<List<T>, List<T>> distinctSequence(Function<? super T, R> keySelector) {
        return source -> source.scan(new Distinct<>(new HashSet<R>(), new ArrayList<T>()), (acc, item) -> {
            var newItem = new ArrayList<T>();
            item.forEach(i -> {
                try {
                    if (acc.previous.add(keySelector.apply(i)))
                        newItem.add(i);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return new Distinct<>(acc.previous, newItem);
        })
                .skip(1)
                .map(md -> md.current);
    }


    public static <T, K> SingleTransformer<T, T> cachedShared(Cache<K, Notification<T>> sharedCache, K key) {
        return source -> Single.defer(() -> {
            var fromCached = sharedCache.getIfPresent(key);
            if (fromCached == null)
                return source
                        .doOnSuccess(l -> sharedCache.put(key, Notification.createOnNext(l)))
                        .doOnError(e -> sharedCache.put(key, Notification.createOnError(e)));
            else if (fromCached.isOnNext())
                return Single.just(fromCached.getValue());
            else
                return Single.error(fromCached.getError());
        });

    }


}
