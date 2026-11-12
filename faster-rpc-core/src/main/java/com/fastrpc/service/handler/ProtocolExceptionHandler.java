package com.fastrpc.service.handler;

import com.fastrpc.constant.MessageType;
import com.fastrpc.exception.ProtocolException;
import com.fastrpc.protocol.Packet;
import com.fastrpc.protocol.ResultFrame;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@ChannelHandler.Sharable
public class ProtocolExceptionHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof ProtocolException protocolException && protocolException.getRequestId() != 0) {
            ResultFrame resultFrame = new ResultFrame(null, protocolException);
            ctx.writeAndFlush(new Packet(resultFrame, protocolException.getRequestId(), MessageType.RESPONSE.getCode()));
            return;
        }
        ctx.fireExceptionCaught(cause);
    }
}
