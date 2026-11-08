package com.fastrpc.service.handler;

import com.fastrpc.protocol.ResultFrame;
import com.fastrpc.proxy.InFlight;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;

import java.nio.channels.ClosedChannelException;
import java.util.Map;

@ChannelHandler.Sharable
public class ResultHandler extends SimpleChannelInboundHandler<Map<String, Object>> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Map<String, Object> msg) {
        InFlight inFlight = ctx.channel().attr(InFlight.KEY).get();
        if (inFlight == null) {
            return;
        }
        int requestId = (int) msg.get("requestId");
        Promise<ResultFrame> promise = inFlight.remove(requestId);
        if (promise == null) {
            return;
        }
        Object frame = msg.get("frame");
        if (frame instanceof ResultFrame resultFrame) {
            promise.trySuccess(resultFrame);
        } else {
            promise.tryFailure(new IllegalStateException("illegal format of response " + requestId));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        InFlight inFlight = ctx.channel().attr(InFlight.KEY).get();
        if (inFlight != null) {
            inFlight.failAll(new ClosedChannelException());
        }
        ctx.fireChannelInactive();
    }
}
