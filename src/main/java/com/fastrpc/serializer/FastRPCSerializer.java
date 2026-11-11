package com.fastrpc.serializer;

import com.fastrpc.codec.Codec;
import com.fastrpc.codec.impl.HessianCodec;
import com.fastrpc.protocol.Frame;
import com.fastrpc.protocol.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@ChannelHandler.Sharable
public class FastRPCSerializer extends MessageToMessageCodec<ByteBuf, Packet> {
    private final Codec<Frame> codec = new HessianCodec();


    /*
     * | field     | bytes  | value                                                                 |
     * | --------- | ------ | --------------------------------------------------------------------- |
     * | magic     | 4      | UTF-8 "frpc"                                                          |
     * | version   | 1      | Frame.VERSION, currently 1                                            |
     * | type      | 1      | 1 request, 2 response, 3 ping, 4 pong                                 |
     * | flags     | 1      | 0 none; 1 cancel the call with this requestId, body length 0         |
     * | requestId | 4      | call id; also matches a ping with its pong                            |
     * | length    | 4      | body size in bytes; 0 when frame is null (ping and pong)              |
     * | body      | length | Hessian-encoded Frame: DataFrame for a request, ResultFrame for a response |
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, List out) throws Exception {
        try {
            ByteBuf buffer = ctx.alloc().buffer();
            buffer.writeBytes(Frame.MAGIC_NUMBER.getBytes(StandardCharsets.UTF_8));
            buffer.writeByte(Frame.VERSION);
            buffer.writeByte(msg.getType());
            buffer.writeByte(msg.getFlags());
            buffer.writeInt(msg.getRequestId());
            if(msg.getFrame() != null){
                byte[] bytes = codec.encode(msg.getFrame());
                buffer.writeInt(bytes.length);
                buffer.writeBytes(bytes);
            }else{
                buffer.writeInt(0);
            }
            out.add(buffer);
        } catch (Exception e) {
            log.error("Error while encoding {}", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List out) throws Exception {
        try {
            CharSequence magicNum = msg.readCharSequence(4, StandardCharsets.UTF_8);
            byte version = msg.readByte();
            byte type = msg.readByte();
            byte flag = msg.readByte();
            int requestId = msg.readInt();

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("magicNum", magicNum);
            attributes.put("version", version);
            attributes.put("type", type);
            attributes.put("flag", flag);
            attributes.put("requestId", requestId);

            int length = msg.readInt();
            if(length != 0){
                byte[] bytes = new byte[length];
                msg.readBytes(bytes);
                Frame frame = codec.decode(bytes);
                attributes.put("frame", frame);
            }
            out.add(attributes);

        } catch (Exception e) {
            log.error("Error while decoding {}", e);
            throw new RuntimeException(e);
        }

    }
}
