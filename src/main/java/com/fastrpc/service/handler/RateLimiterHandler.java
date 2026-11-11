package com.fastrpc.service.handler;

import com.fastrpc.config.ServiceRateLimiter;
import com.fastrpc.constant.Flag;
import com.fastrpc.constant.MessageType;
import com.fastrpc.exception.ProtocolException;
import com.fastrpc.limiter.TokenBucket;
import com.fastrpc.protocol.DataFrame;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@ChannelHandler.Sharable
public class RateLimiterHandler extends SimpleChannelInboundHandler<Map<String, Object>> {

    ServiceRateLimiter serviceRateLimiter;

    public RateLimiterHandler(ServiceRateLimiter serviceRateLimiter) {
        this.serviceRateLimiter = serviceRateLimiter;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Map<String, Object> msg) throws Exception {
        byte type = (byte) msg.get("type");
        byte flag = (byte) msg.get("flag");
        int requestId = (int) msg.get("requestId");
        DataFrame frame = (DataFrame) msg.get("frame");
        if(flag == Flag.CANCEL) {
            ctx.fireChannelRead(msg);
            return;
        }
        Class<?> serviceInterface = frame.getServiceInterface();
        MessageType messageType = MessageType.from(type);
        if (messageType == MessageType.REQUEST) {
            TokenBucket tokenBucket = this.serviceRateLimiter.get(serviceInterface.getName());
            if (tokenBucket.tryAcquire()) {
                ctx.fireChannelRead(msg);
            }else{
                throw new ProtocolException(requestId, "Rate limit exceeded for service " + serviceInterface.getName());
            }
        }
    }
}
