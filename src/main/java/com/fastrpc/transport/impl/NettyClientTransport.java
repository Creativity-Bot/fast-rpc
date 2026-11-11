package com.fastrpc.transport.impl;

import com.fastrpc.client.impl.NettyClient;
import com.fastrpc.constant.MessageType;
import com.fastrpc.exception.TimeoutException;
import com.fastrpc.exception.WriteFailureException;
import com.fastrpc.protocol.DataFrame;
import com.fastrpc.protocol.Frame;
import com.fastrpc.protocol.Packet;
import com.fastrpc.protocol.ResultFrame;
import com.fastrpc.proxy.InFlight;
import com.fastrpc.transport.Transport;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Promise;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@Component
public class NettyClientTransport implements Transport {

    @Override
    public Object send(InetSocketAddress address, Class<?> target, Method method, Object[] args) throws Throwable {
        Channel channel = NettyClient.get(address.getHostName(), address.getPort());

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

        if (!promise.await(3, TimeUnit.SECONDS)) {
            inFlight.remove(nextRequestId);
            if (promise.tryFailure(new TimeoutException("rpc timeout"))) {
                throw new TimeoutException("rpc timeout");
            }
        }

        ResultFrame result = promise.sync().getNow();
        if (result.getException() != null) {
            throw result.getException();
        }
        return result.getResult();
    }
}
