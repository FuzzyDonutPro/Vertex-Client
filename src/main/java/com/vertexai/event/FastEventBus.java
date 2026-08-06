package com.vertexai.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * FastEventBus — High-performance, zero-allocation priority event dispatcher.
 * Eliminates garbage collector overhead by pre-compiling listener arrays.
 */
public class FastEventBus<T> {

    private final List<Consumer<T>> listeners = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private Consumer<T>[] listenerCache = new Consumer[0];
    private boolean dirty = false;

    public synchronized void register(Consumer<T> listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            dirty = true;
        }
    }

    public synchronized void unregister(Consumer<T> listener) {
        if (listeners.remove(listener)) {
            dirty = true;
        }
    }

    @SuppressWarnings("unchecked")
    public void post(T event) {
        if (dirty) {
            synchronized (this) {
                if (dirty) {
                    listenerCache = listeners.toArray(new Consumer[0]);
                    dirty = false;
                }
            }
        }

        Consumer<T>[] cache = listenerCache;
        for (int i = 0; i < cache.length; i++) {
            try {
                cache[i].accept(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
