package com.fastrpc.test;

import com.fastrpc.constant.MessageType;
import com.fastrpc.protocol.DataFrame;
import com.fastrpc.protocol.Packet;
import com.fastrpc.serializer.FastRPCSerializer;
import com.fastrpc.service.handler.FrameHandler;
import com.fastrpc.validation.ProtocolValidator;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class FrpcClient {
    @SneakyThrows
    public static void main(String[] args) {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
        EventLoopGroup groupHandler = new DefaultEventLoopGroup();
        EventLoopGroup serviceGroup = new DefaultEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100);
        bootstrap.group(eventLoopGroup);
        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new IdleStateHandler(15, 10, 0));
                pipeline.addLast(new LengthFieldBasedFrameDecoder(1024, 11, 4, 0, 0));
                pipeline.addLast(LOGGING_HANDLER);
                pipeline.addLast(groupHandler, new FastRPCSerializer());
                pipeline.addLast(groupHandler, new ChannelDuplexHandler(){
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
                            }else if (((IdleStateEvent) evt).state() == IdleState.WRITER_IDLE){
                                log.debug("detect write idle so fire ping ...");
                                ctx.channel().writeAndFlush(new Packet(null, 123, MessageType.PING.getCode()));
                            }
                        }
                    }

                });
                pipeline.addLast(groupHandler, new ProtocolValidator());
                pipeline.addLast(serviceGroup, new FrameHandler(null));
                pipeline.addLast(new ChannelInboundHandlerAdapter(){
                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        DataFrame dataFrame = new DataFrame(
                                HelloService.class,
                                "hello",
                                new Class<?>[]{String.class},
                                new Object[]{"faster-rpc"}
                        );
                        ctx.writeAndFlush(new Packet(dataFrame, 1, MessageType.REQUEST.getCode()));
                    }
                });
            }
        });
        Channel channel = bootstrap.connect(new InetSocketAddress(8000)).sync().channel();
        channel.closeFuture().sync();
    }
}
