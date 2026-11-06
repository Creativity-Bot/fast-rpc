package com.fastrpc.service.handler;

import com.fastrpc.constant.MessageType;
import com.fastrpc.protocol.Frame;
import com.fastrpc.protocol.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class PacketHandler extends SimpleChannelInboundHandler<Packet> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet msg) throws Exception {
        msg.setType(MessageType.PONG.getCode());
        ctx.writeAndFlush(msg);
    }
}
