package org.zella.tuapse.providers;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.Test;
import org.zella.tuapse.model.torrent.TFile;

import java.util.List;

public class RxUtilsTest {

    @Test
    public void distinctSequenceShouldWork()   {

        Observable<List<String>> source = Observable.just(
                List.of("a", "c", "e"),
                List.of("a", "b", "c", "d"),
                List.of("d", "e", "f")
        )
        .compose(RxUtils.distinctSequence(o -> o));


        var observer = new TestObserver<List<String>>();

        source.subscribe(observer);


        System.out.println(observer.values());
    }
}
