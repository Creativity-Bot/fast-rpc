package com.fastrpc.validation;

import com.fastrpc.constant.Flag;
import com.fastrpc.constant.MessageType;
import com.fastrpc.exception.ProtocolException;
import com.fastrpc.protocol.Frame;
import com.fastrpc.protocol.Packet;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@ChannelHandler.Sharable
public class ProtocolValidator extends SimpleChannelInboundHandler<Map<String, Object>> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Map msg) throws Exception {

        // validate the attributes from the map
        String magicNum = (String) msg.get("magicNum");
        byte version = (byte) msg.get("version");
        byte type = (byte) msg.get("type");
        byte flag = (byte) msg.get("flag");
        int requestId = (int) msg.get("requestId");
        MessageType messageType = MessageType.from(type);

        if (magicNum == null || magicNum.length() != 4) {
            log.error("Magic number is not valid for request with id {}", requestId);
            throw new ProtocolException(requestId, "Magic number is not valid");
        }
        if (version != Frame.VERSION) {
            log.error("Version is not valid for request with id {}", requestId);
            throw new ProtocolException(requestId, "Version is not valid");
        }
        if (messageType == null) {
            log.error("Type is invalid for request with id {}", requestId);
            throw new ProtocolException(requestId, "Type is invalid");
        }
        if (flag != Flag.NONE && flag != Flag.CANCEL) {
            log.error("Flag is invalid for request with id {}", requestId);
            throw new ProtocolException(requestId, "Flag is invalid");
        }
        if (requestId == 0) {
            log.error("Request ID is required for request with id {}", requestId);
            throw new ProtocolException(requestId, "Request ID is required");
        }

        if (messageType == MessageType.PING) {
            ctx.fireChannelRead(new Packet(null, requestId, type));
        } else if(messageType == MessageType.PONG){
            log.debug("pong received from request with id {}", requestId);
        } else {
            // request or response
            ctx.fireChannelRead(msg);
        }

    }
}
