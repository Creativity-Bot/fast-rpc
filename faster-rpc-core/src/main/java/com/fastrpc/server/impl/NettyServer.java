package com.fastrpc.server.impl;

import com.fastrpc.config.ServiceMap;
import com.fastrpc.config.ServiceRateLimiter;
import com.fastrpc.constant.ClientCall;
import com.fastrpc.registry.Registry;
import com.fastrpc.serializer.FastRPCSerializer;
import com.fastrpc.server.Server;
import com.fastrpc.service.handler.*;
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

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@Slf4j
public class NettyServer implements Server {

    private final int workerNumbers;

    private final int handlerNumbers;

    private final int serviceNumbers;

    private final ServiceMap serviceMap;

    private final ServiceRateLimiter serviceRateLimiter;

    private final EventLoopGroup bossGroup;

    private final EventLoopGroup workerGroup;

    private final EventLoopGroup handlerGroup;

    private String ip;

    private int port;

    private Long readIdleTime;

    private long writeIdleTime;

    private TimeUnit timeUnit;

    private FrameHandler frameHandler;

    private Registry registry;

    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public NettyServer(int workerNumbers, int handlerNumbers, int serviceNumbers, ServiceMap serviceMap, ServiceRateLimiter serviceRateLimiter, Registry registry) {
        this(workerNumbers, handlerNumbers, serviceNumbers, serviceMap, serviceRateLimiter, registry, "localhost", 9527, 15, 0, TimeUnit.SECONDS);
    }

    @Builder
    public NettyServer(int workerNumbers, int handlerNumbers, int serviceNumbers, ServiceMap serviceMap, ServiceRateLimiter serviceRateLimiter, Registry registry, String ip, int port, long readIdleTime, long writeIdleTime, TimeUnit timeUnit) {
        this.workerNumbers = workerNumbers;
        this.handlerNumbers = handlerNumbers;
        this.serviceNumbers = serviceNumbers;
        this.serviceMap = serviceMap;
        this.serviceRateLimiter = serviceRateLimiter;
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup(workerNumbers);
        this.handlerGroup = new DefaultEventLoopGroup(handlerNumbers);
        this.readIdleTime = readIdleTime;
        this.writeIdleTime = writeIdleTime;
        this.timeUnit = timeUnit;
        this.ip = ip;
        this.port = port;
        this.registry = registry;
    }

    @Override
    public void start() {

        LoggingHandler loggingHandler = new LoggingHandler();
        FastRPCSerializer fastRPCSerializer = new FastRPCSerializer();
        ProtocolValidator protocolValidator = new ProtocolValidator();
        frameHandler = new FrameHandler(this.serviceMap, this.serviceNumbers);
        PacketHandler packetHandler = new PacketHandler();
        HeartBeatHandler heartBeatHandler = new HeartBeatHandler();
        ProtocolExceptionHandler protocolExceptionHandler = new ProtocolExceptionHandler();
        RateLimiterHandler rateLimiterHandler = new RateLimiterHandler(this.serviceRateLimiter);

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(bossGroup, workerGroup);
            serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    if (ch.attr(ClientCall.CALLS).get() == null) {
                        ch.attr(ClientCall.CALLS).setIfAbsent(new ConcurrentHashMap<>());
                    }

                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new IdleStateHandler(readIdleTime, writeIdleTime, 0, timeUnit));
                    pipeline.addLast(new LengthFieldBasedFrameDecoder(4*1024*1024, 11, 4, 0, 0));
                    pipeline.addLast(loggingHandler);
                    pipeline.addLast(handlerGroup, fastRPCSerializer);
                    pipeline.addLast(handlerGroup, heartBeatHandler);
                    pipeline.addLast(handlerGroup, protocolValidator);
                    pipeline.addLast(handlerGroup, rateLimiterHandler);
                    pipeline.addLast(handlerGroup, protocolExceptionHandler);
                    pipeline.addLast(handlerGroup, frameHandler);
                    pipeline.addLast(handlerGroup, packetHandler);
                }
            });


            ChannelFuture channelFuture = serverBootstrap.bind(ip, port);
            channelFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if (future.isSuccess()) {
                        // register services
                        registerServices(ip, port);

                        //listen for closure
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
        } catch (Exception e) {
            log.error("server failed to init due to {}", e);
             stop();
        }
    }

    private void registerServices(String ip, int port) {
        Iterator iterator = serviceMap.getServiceSet().iterator();
        while (iterator.hasNext()) {
            String service = (String) iterator.next();
            registry.registerService(service, new InetSocketAddress(ip, port));
        }
    }

    @Override
    public void stop() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        registry.shutdown();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        handlerGroup.shutdownGracefully();
        if (frameHandler != null) {
            frameHandler.close();
        }
    }
}
