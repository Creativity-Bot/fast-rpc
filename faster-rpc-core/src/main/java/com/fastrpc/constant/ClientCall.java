package com.fastrpc.constant;

import io.netty.util.AttributeKey;
import java.util.concurrent.Future;
import java.util.Map;

public class ClientCall {
    public static final AttributeKey<Map<Integer, Future<?>>> CALLS = AttributeKey.valueOf("frpc.calls");

}
