package com.fastrpc.service.handler;

import com.fastrpc.constant.MessageType;
import com.fastrpc.protocol.DataFrame;
import com.fastrpc.protocol.Frame;
import com.fastrpc.protocol.Packet;
import com.fastrpc.protocol.ResultFrame;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;

@Slf4j
public class FrameHandler extends SimpleChannelInboundHandler<Map<String, Object>> {
    private final Map<String, Object> serviceMap;

    public FrameHandler(Map<String, Object> serviceMap) {
        this.serviceMap = serviceMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Map<String, Object> msg) throws Exception {

        byte type = (byte) msg.get("type");
        int requestId = (int) msg.get("requestId");
        MessageType messageType = MessageType.from(type);
        if (messageType == MessageType.REQUEST) {
            try {
                Frame frame = invokeMethod(msg);
                ctx.channel().writeAndFlush(new Packet(frame, requestId, MessageType.RESPONSE.getCode()));
            } catch (Exception e) {
                ctx.channel().writeAndFlush(new Packet(null, requestId, MessageType.RESPONSE.getCode()));
            }
        } else {
            ResultFrame frame = (ResultFrame) msg.get("frame");
            log.debug("response received : {}", frame.getResult());
        }
    }

    private Frame invokeMethod(Map<String, Object> msg) {
        DataFrame frame = (DataFrame) msg.get("frame");
        Class<?> serviceInterface = frame.getServiceInterface();
        String methodName = frame.getMethodName();
        Class<?>[] parameterTypes = frame.getParameterTypes();
        Object[] args = frame.getArgs();

        String serviceName = serviceInterface.getName();
        Object orDefault = serviceMap.getOrDefault(serviceName, null);
        if (orDefault == null) {
            log.error("service {} does not exist", serviceName);
            throw new RuntimeException("service does not exist");
        } else {
            ResultFrame resultFrame = new ResultFrame();
            try {
                Method method = serviceInterface.getDeclaredMethod(methodName, parameterTypes);
                Object result = method.invoke(orDefault, args);
                log.debug("result is {}", result);
                resultFrame.setResult(result);
            } catch (Exception e) {
                log.error("method invocation occurs error {}", methodName);
                throw new RuntimeException(e);
            }
            return resultFrame;
        }
    }
}
