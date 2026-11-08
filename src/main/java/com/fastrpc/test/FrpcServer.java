package com.fastrpc.test;

import com.fastrpc.config.ServiceRegistry;
import com.fastrpc.serializer.FastRPCSerializer;
import com.fastrpc.service.handler.FrameHandler;
import com.fastrpc.service.handler.PacketHandler;
import com.fastrpc.validation.ProtocolValidator;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class FrpcServer {
    public static void main(String[] args) {

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        EventLoopGroup handlerGroup = new DefaultEventLoopGroup(16);
        EventLoopGroup serviceGroup = new DefaultEventLoopGroup(16);

        ServiceRegistry serviceRegistry = new ServiceRegistry();
        serviceRegistry.register(HelloService.class.getName(), new HelloServiceImpl());

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(bossGroup, workerGroup);
            serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new IdleStateHandler(15, 0, 0));
                    pipeline.addLast(new LengthFieldBasedFrameDecoder(1024, 11, 4, 0, 0));
                    pipeline.addLast(new LoggingHandler());
                    pipeline.addLast(handlerGroup, new FastRPCSerializer());
                    pipeline.addLast(handlerGroup, new ChannelDuplexHandler(){
                        @Override
                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                            // if the event is idle, close the channel
                            if (evt instanceof IdleStateEvent) {
                                ctx.channel().closeFuture().addListener(new ChannelFutureListener() {
                                    @Override
                                    public void operationComplete(ChannelFuture future) throws Exception {
                                        if(future.isSuccess()){
                                            log.debug("channel has been shut down due to read timeout");
                                        }
                                    }
                                });
                            }
                        }
                    });
                    pipeline.addLast(handlerGroup, new ProtocolValidator());
                    pipeline.addLast(serviceGroup, new FrameHandler(serviceRegistry, 16));
                    pipeline.addLast(serviceGroup, new PacketHandler());
                }
            });

            Channel channel = serverBootstrap.bind(8000).sync().channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("server failed to init due to {}", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            handlerGroup.shutdownGracefully();
            serviceGroup.shutdownGracefully();
        }
    }
}
