package org.zella.tuapse.providers;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class LifoBlockingDeque<E> extends LinkedBlockingDeque<E> {

    @Override
    public boolean offer(E e) {
        // Override to put objects at the front of the list
        return super.offerFirst(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        // Override to put objects at the front of the list
        return super.offerFirst(e, timeout, unit);
    }


    @Override
    public boolean add(E e) {
        // Override to put objects at the front of the list
        return super.offerFirst(e);
    }

    @Override
    public void put(E e) throws InterruptedException {
        //Override to put objects at the front of the list
        super.putFirst(e);
    }
}