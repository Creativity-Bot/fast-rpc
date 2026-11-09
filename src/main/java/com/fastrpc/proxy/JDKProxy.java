package com.fastrpc.proxy;

import com.fastrpc.client.impl.NettyClient;
import com.fastrpc.constant.MessageType;
import com.fastrpc.exception.ConnectException;
import com.fastrpc.exception.WriteFailureException;
import com.fastrpc.protocol.DataFrame;
import com.fastrpc.protocol.Frame;
import com.fastrpc.protocol.Packet;
import com.fastrpc.protocol.ResultFrame;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Promise;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

public class JDKProxy implements InvocationHandler {
    private static final int MAX_ATTEMPTS = 3;
    private static final int BACKOFF_PERIOD = 1;
    private static final int BACKOFF_MULTIPLIER = 2;

    private Class<?> target;
    private String ip;
    private int port;


    public JDKProxy(Class<?> target, String ip, int port) {
        this.target = target;
        this.ip = ip;
        this.port = port;
    }

    public Object createProxy() {
        return Proxy.newProxyInstance(target.getClassLoader(), new Class[]{target}, this);
    }




    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long backoffSeconds = BACKOFF_PERIOD;
        for (int attempt = 1; ; attempt++) {
            try {
                return send(method, args);
            } catch (ConnectException | WriteFailureException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw e;
                }
                try {
                    TimeUnit.SECONDS.sleep(backoffSeconds);
                    backoffSeconds *= BACKOFF_MULTIPLIER;
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw interruptedException;
                }
            }
        }
    }

    private Object send(Method method, Object[] args) throws Throwable {
        Channel channel = NettyClient.get(ip, port);

        if (channel.attr(InFlight.KEY).get() == null) {
            channel.attr(InFlight.KEY).setIfAbsent(new InFlight());
        }

        InFlight inFlight = channel.attr(InFlight.KEY).get();
        int nextRequestId = inFlight.nextId();

        Promise<ResultFrame> promise = channel.eventLoop().newPromise();
        inFlight.add(nextRequestId, promise);

        Frame dataFrame = new DataFrame(target, method.getName(), method.getParameterTypes(), args);
        Packet packet = new Packet(dataFrame, nextRequestId, MessageType.REQUEST.getCode());

        channel.writeAndFlush(packet).addListener((ChannelFutureListener) writeFuture -> {
            if (!writeFuture.isSuccess()) {
                inFlight.remove(nextRequestId);
                promise.tryFailure(new WriteFailureException(writeFuture.cause()));
            }
        });

        ResultFrame result = promise.sync().getNow();
        if (result.getException() != null) {
            throw result.getException();
        }
        return result.getResult();
    }
}
