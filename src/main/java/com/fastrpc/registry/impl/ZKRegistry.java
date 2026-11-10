package com.fastrpc.registry.impl;

import com.fastrpc.registry.Registry;
import com.fastrpc.registry.ServiceListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ZKRegistry implements Registry {

    private final CuratorFramework client;


    public ZKRegistry(String zkServerAddress) {
        this.client = CuratorFrameworkFactory.newClient(zkServerAddress, 1000*60, 1000*15, new ExponentialBackoffRetry(1000, 3, 3000));
        this.client.start();
    }

    @Override
    public void registerService(String targetInterfaceName, InetSocketAddress hostAndPort) {
        String servicePath = servicePath(targetInterfaceName);
        try {
            if (client.checkExists().forPath(servicePath) == null) {
                this.client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(servicePath);
            }
            String nodeUrl = this.client.create().withMode(CreateMode.EPHEMERAL).forPath(servicePath + "/" + hostAndPort.getHostName() + ":" + hostAndPort.getPort());

            log.debug("node is created {} ", nodeUrl);
        } catch (Exception e) {
            log.error("node created error ", e);
            throw new RuntimeException("register service to zookeeper failed");
        }
    }

    private List<InetSocketAddress> mapPathToSocketAddr(List<String> serviceAddresses) {
        return serviceAddresses.stream()
                .map(s -> s.split(":"))
                .map(s -> new InetSocketAddress(s[0], Integer.parseInt(s[1])))
                .collect(Collectors.toList());
    }

    @Override
    public void subscribeService(String targetInterfaceName, ServiceListener listener) {
        String serviceFullPath = servicePath(targetInterfaceName);

        // init service for the first time
        try {
            List<String> newNodes = client.getChildren().forPath(serviceFullPath);
            listener.onAddresses(mapPathToSocketAddr(newNodes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // add listener on service for update in the future
        CuratorCache curatorCache = CuratorCache.build(client, serviceFullPath);
        CuratorCacheListener curatorCacheListener = CuratorCacheListener.builder()
                .forPathChildrenCache(serviceFullPath, client, (client, event) -> {
                    PathChildrenCacheEvent.Type type = event.getType();
                    if (type == PathChildrenCacheEvent.Type.CHILD_ADDED
                            || type == PathChildrenCacheEvent.Type.CHILD_REMOVED
                            || type == PathChildrenCacheEvent.Type.CHILD_UPDATED
                            || type == PathChildrenCacheEvent.Type.INITIALIZED) {
                        try {
                            List<String> newNodes = client.getChildren().forPath(serviceFullPath);
                            listener.onAddresses(List.copyOf(mapPathToSocketAddr(newNodes)));
                        } catch (RuntimeException e) {
                            log.error("service watch update failed", e);
                        }
                    }
                }).build();
        curatorCache.listenable().addListener(curatorCacheListener);
        curatorCache.start();
    }

    private String servicePath(String targetInterfaceName) {
        return SERVICE_PREFIX + "/" + targetInterfaceName + SERVICE_SUFFIX;
    }
}
