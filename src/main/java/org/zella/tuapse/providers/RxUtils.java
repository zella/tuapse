package org.zella.tuapse.providers;

import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Function;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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

}
