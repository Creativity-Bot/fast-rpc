package com.fastrpc.proxy;

import com.fastrpc.protocol.ResultFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Promise;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class InFlight {
    public static final AttributeKey<InFlight> KEY = AttributeKey.valueOf("inFlight");

    private final AtomicInteger nextId = new AtomicInteger();
    private final ConcurrentHashMap<Integer, Promise<ResultFrame>> pending = new ConcurrentHashMap<>();

    public int nextId() {
        return nextId.updateAndGet(id -> id == Integer.MAX_VALUE ? 1 : id + 1);
    }

    public void add(int requestId, Promise<ResultFrame> promise) {
        pending.put(requestId, promise);
    }

    public Promise<ResultFrame> remove(int requestId) {
        return pending.remove(requestId);
    }

    public void failAll(Throwable cause) {
        for (Integer requestId : new ArrayList<>(pending.keySet())) {
            Promise<ResultFrame> promise = pending.remove(requestId);
            if (promise != null) {
                promise.tryFailure(cause);
            }
        }
    }
}
