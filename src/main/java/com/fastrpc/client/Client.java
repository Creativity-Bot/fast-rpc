package com.fastrpc.client;

import io.netty.channel.Channel;

public interface Client {
    Channel connect() throws Exception;
    void close();
}
