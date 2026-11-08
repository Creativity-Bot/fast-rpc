package com.fastrpc.server.impl;

import com.fastrpc.config.ServiceRegistry;
import com.fastrpc.serializer.FastRPCSerializer;
import com.fastrpc.server.Server;
import com.fastrpc.service.handler.FrameHandler;
import com.fastrpc.service.handler.HeartBeatHandler;
import com.fastrpc.service.handler.PacketHandler;
import com.fastrpc.validation.ProtocolValidator;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class NettyServer implements Server {

    private final int workerNumbers;

    private final int handlerNumbers;

    private final int serviceNumbers;

    private final ServiceRegistry serviceRegistry;

    private final EventLoopGroup bossGroup;

    private final EventLoopGroup workerGroup;

    private final EventLoopGroup handlerGroup;

    private String ip;

    private int port;

    private Long readIdleTime;

    private long writeIdleTime;

    private TimeUnit timeUnit;

    private FrameHandler frameHandler;

    public NettyServer(int workerNumbers, int handlerNumbers, int serviceNumbers, ServiceRegistry serviceRegistry) {
        this(workerNumbers, handlerNumbers, serviceNumbers, serviceRegistry, "localhost", 9527, 15, 0, TimeUnit.SECONDS);
    }

    @Builder
    private NettyServer(int workerNumbers, int handlerNumbers, int serviceNumbers, ServiceRegistry serviceRegistry, String ip, int port, long readIdleTime, long writeIdleTime, TimeUnit timeUnit) {
        this.workerNumbers = workerNumbers;
        this.handlerNumbers = handlerNumbers;
        this.serviceNumbers = serviceNumbers;
        this.serviceRegistry = serviceRegistry;
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup(workerNumbers);
        this.handlerGroup = new DefaultEventLoopGroup(handlerNumbers);
        this.readIdleTime = readIdleTime;
        this.writeIdleTime = writeIdleTime;
        this.timeUnit = timeUnit;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void start() {

        LoggingHandler loggingHandler = new LoggingHandler();
        FastRPCSerializer fastRPCSerializer = new FastRPCSerializer();
        ProtocolValidator protocolValidator = new ProtocolValidator();
        frameHandler = new FrameHandler(this.serviceRegistry, this.serviceNumbers);
        PacketHandler packetHandler = new PacketHandler();
        HeartBeatHandler heartBeatHandler = new HeartBeatHandler();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(bossGroup, workerGroup);
            serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new IdleStateHandler(readIdleTime, writeIdleTime, 0, timeUnit));
                    pipeline.addLast(new LengthFieldBasedFrameDecoder(1024, 11, 4, 0, 0));
                    pipeline.addLast(loggingHandler);
                    pipeline.addLast(handlerGroup, fastRPCSerializer);
                    pipeline.addLast(handlerGroup, heartBeatHandler);
                    pipeline.addLast(handlerGroup, protocolValidator);
                    pipeline.addLast(handlerGroup, frameHandler);
                    pipeline.addLast(handlerGroup, packetHandler);
                }
            });


            ChannelFuture channelFuture = serverBootstrap.bind(ip, port);
            channelFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if (future.isSuccess()) {
                        //2服务注册功能
                        //registerServices(InetAddress.getLocalHost().getHostAddress(), port, exposeBeans, registry);
                        //isStarted = true;

                        //监听关闭
                        Channel channel = channelFuture.channel();
                        ChannelFuture closeFuture = channel.closeFuture();
                        closeFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
                            @Override
                            public void operationComplete(Future<? super Void> future) throws Exception {
                                if (future.isSuccess()) {
                                    stop();
                                }
                            }
                        });
                    }
                }
            });

            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        } catch (Exception e) {
            log.error("server failed to init due to {}", e);
             stop();
        }
    }

    @Override
    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        handlerGroup.shutdownGracefully();
        frameHandler.close();
    }
}
