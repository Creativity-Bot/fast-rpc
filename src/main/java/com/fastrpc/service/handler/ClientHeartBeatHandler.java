package com.fastrpc.service.handler;

import com.fastrpc.constant.MessageType;
import com.fastrpc.protocol.Packet;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class ClientHeartBeatHandler extends ChannelDuplexHandler {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // if the event is idle, close the channel
        if (evt instanceof IdleStateEvent) {
            if(((IdleStateEvent) evt).state() == IdleState.READER_IDLE){
                ctx.channel().closeFuture().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if(future.isSuccess()){
                            log.debug("channel has been shut down due to read timeout");
                        }
                    }
                });
                ctx.channel().close();
            }else if (((IdleStateEvent) evt).state() == IdleState.WRITER_IDLE){
                log.debug("detect write idle so fire ping ...");
                ctx.channel().writeAndFlush(new Packet(null, 123, MessageType.PING.getCode()));
            }
        }
    }
}
