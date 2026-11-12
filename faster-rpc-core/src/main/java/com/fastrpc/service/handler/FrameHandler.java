package com.fastrpc.service.handler;

import com.fastrpc.config.ServiceMap;
import com.fastrpc.constant.ClientCall;
import com.fastrpc.constant.Flag;
import com.fastrpc.constant.MessageType;
import com.fastrpc.protocol.DataFrame;
import com.fastrpc.protocol.Frame;
import com.fastrpc.protocol.Packet;
import com.fastrpc.protocol.ResultFrame;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@ChannelHandler.Sharable
public class FrameHandler extends SimpleChannelInboundHandler<Map<String, Object>> {
    private final ServiceMap serviceMap;
    private final int serviceNumber;
    private final ThreadPoolExecutor servicesPool;

    public FrameHandler(ServiceMap serviceMap, int serviceNumber) {
        this.serviceMap = serviceMap;
        this.serviceNumber = serviceNumber;
        this.servicesPool = new ThreadPoolExecutor(
                this.serviceNumber,
                this.serviceNumber,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(128),
                new DefaultThreadFactory("frpc-business"),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Map<String, Object> msg) throws Exception {

        Map<Integer, Future<?>> map = ctx.channel().attr(ClientCall.CALLS).get();
        byte type = (byte) msg.get("type");
        byte flag = (byte) msg.get("flag");
        int requestId = (int) msg.get("requestId");
        if (flag == Flag.CANCEL) {
            Future<?> future = map.get(requestId);
            if (future != null) {
                future.cancel(true);
                map.remove(requestId);
            }
            return;
        }

        MessageType messageType = MessageType.from(type);
        if (messageType == MessageType.REQUEST) {
            try {
                Future<?> future = servicesPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Frame frame = invokeMethod(msg);
                            if(Thread.currentThread().isInterrupted()){
                                return;
                            }
                            ctx.channel().writeAndFlush(new Packet(frame, requestId, MessageType.RESPONSE.getCode()));
                        } catch (Exception e) {
                            Throwable cause = e instanceof InvocationTargetException ? e.getCause() : e;
                            if (cause instanceof InterruptedException) {
                                return;
                            }
                            ctx.channel().writeAndFlush(new Packet(new ResultFrame(null, (Exception) cause), requestId, MessageType.RESPONSE.getCode()));
                        } finally {
                            map.remove(requestId);
                        }
                    }
                });
                map.put(requestId, future);
            } catch (Exception e) {
                ctx.channel().writeAndFlush(new Packet(new ResultFrame(null, e), requestId, MessageType.RESPONSE.getCode()));
            }
        }
    }

    private Frame invokeMethod(Map<String, Object> msg) throws Exception{
        DataFrame frame = (DataFrame) msg.get("frame");
        Class<?> serviceInterface = frame.getServiceInterface();
        String methodName = frame.getMethodName();
        Class<?>[] parameterTypes = frame.getParameterTypes();
        Object[] args = frame.getArgs();

        String serviceName = serviceInterface.getName();
        Object orDefault = serviceMap.get(serviceName);
        if (orDefault == null) {
            log.error("service {} does not exist", serviceName);
            throw new RuntimeException("service does not exist");
        } else {
            ResultFrame resultFrame = new ResultFrame();
            Method method = serviceInterface.getDeclaredMethod(methodName, parameterTypes);
            Object result = method.invoke(orDefault, args);
            log.debug("result is {}", result);
            resultFrame.setResult(result);
            return resultFrame;
        }
    }

    public void close(){
        if(servicesPool != null){
            servicesPool.shutdownNow();
        }
    }
}
