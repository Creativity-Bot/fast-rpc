package com.fastrpc.client.impl;

import com.fastrpc.client.Client;
import com.fastrpc.exception.ConnectException;
import com.fastrpc.serializer.FastRPCSerializer;
import com.fastrpc.service.handler.ClientHeartBeatHandler;
import com.fastrpc.service.handler.ResultHandler;
import com.fastrpc.validation.ProtocolValidator;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class NettyClient implements Client {

    private String ip;

    private int port;

    private Long readIdleTime;

    private long writeIdleTime;

    private TimeUnit timeUnit;

    private final EventLoopGroup clientWorker;

    private final EventLoopGroup groupHandler;

    private static final Map<String, Channel> CLIENTS = new ConcurrentHashMap<>();

    public static Channel get(String ip, int port) {
        String key = ip + ":" + port;
        return CLIENTS.computeIfAbsent(key, k -> {
            NettyClient client = new NettyClient(ip, port);
            Channel channel = null;
            try {
                channel = client.connect();
            } catch (Exception e) {
                throw new ConnectException(e);
            }
            return channel;
        });
    }


    private NettyClient(String ip, int port){
        this(ip, port, 15L, 10L, TimeUnit.SECONDS);
    }


    private NettyClient(String ip, int port, Long readIdleTime, long writeIdleTime, TimeUnit timeUnit) {
        this.ip = ip;
        this.port = port;
        this.readIdleTime = readIdleTime;
        this.writeIdleTime = writeIdleTime;
        this.timeUnit = timeUnit;
        this.clientWorker = new NioEventLoopGroup(1);
        this.groupHandler = new DefaultEventLoopGroup(1);
    }

    @Override
    public Channel connect() throws Exception{
        LoggingHandler loggingHandler = new LoggingHandler();
        FastRPCSerializer fastRPCSerializer = new FastRPCSerializer();
        ClientHeartBeatHandler clientHeartBeatHandler = new ClientHeartBeatHandler();
        ProtocolValidator protocolValidator = new ProtocolValidator();
        ResultHandler resultHandler = new ResultHandler();

        Channel channel = null;
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100);
            bootstrap.group(clientWorker);
            bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new IdleStateHandler(15, 10, 0));
                    pipeline.addLast(new LengthFieldBasedFrameDecoder(1024, 11, 4, 0, 0));
                    pipeline.addLast(loggingHandler);
                    pipeline.addLast(groupHandler, fastRPCSerializer);
                    pipeline.addLast(groupHandler, clientHeartBeatHandler);
                    pipeline.addLast(groupHandler, protocolValidator);
                    pipeline.addLast(groupHandler, resultHandler);
                }
            });
            channel = bootstrap.connect(new InetSocketAddress(ip, port)).sync().channel();
            channel.closeFuture().addListener(future -> {
                if (future.isSuccess()) {
                    CLIENTS.remove(ip + ":" + port);
                    close();
                }
            });
            Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        } catch (Exception e) {
            log.error("client failed to init due to {}", e);
            close();
            throw e;
        }

        return channel;
    }

    @Override
    public void close() {
        clientWorker.shutdownGracefully();
        groupHandler.shutdownGracefully();
    }


}
