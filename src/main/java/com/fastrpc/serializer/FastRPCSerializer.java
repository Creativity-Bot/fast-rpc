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
    *
    * | 字段 | 长度 | 取值 |
      | --- | --- | --- |
      | magic | 4 | 仍然是 `frpc` |
      | version | 1 |
      | type | 1 | `1` 请求，`2` 响应，`3` Ping，`4` Pong |
      | flags | 1 | 先写 `0`，留给以后用 |
      | requestId | 8 | 一次调用的编号。Ping / Pong 用来对上同一次探活 |
      | length | 4 | 正文字节数。Ping / Pong 为 `0` |
      | body | length | 请求是 `MethodInvokData`，响应是 `Result`。探活没有正文 |
    *
    *
    *
    * */
    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, List out) throws Exception {
        try {
            ByteBuf buffer = ctx.alloc().buffer();
            buffer.writeBytes(Frame.MAGIC_NUMBER.getBytes(StandardCharsets.UTF_8));
            buffer.writeByte(Frame.VERSION);
            buffer.writeByte(msg.getType());
            buffer.writeByte(0); // safe for future
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
