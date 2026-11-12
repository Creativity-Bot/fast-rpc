package com.fastrpc.autoconfigure;

import com.fastrpc.server.impl.NettyServer;
import org.springframework.context.SmartLifecycle;

public class FrpcServerLifecycle implements SmartLifecycle {

    private final NettyServer server;

    private volatile boolean running;

    public FrpcServerLifecycle(NettyServer server) {
        this.server = server;
    }

    @Override
    public void start() {
        server.start();
        running = true;
    }

    @Override
    public void stop() {
        server.stop();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
