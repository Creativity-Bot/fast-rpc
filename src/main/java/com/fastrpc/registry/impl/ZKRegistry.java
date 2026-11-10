package com.fastrpc.registry.impl;

import com.fastrpc.registry.Registry;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ZKRegistry implements Registry {

    private CuratorFramework client;


    public ZKRegistry(String zkServerAddress) {
        this.client = CuratorFrameworkFactory.newClient(zkServerAddress, 1000*60, 1000*15, new ExponentialBackoffRetry(1000, 3, 3000));
        this.client.start();
    }

    @Override
    public void registerService(String targetInterfaceName, InetSocketAddress hostAndPort) {
        String servicePath = SERVICE_PREFIX + "/" + targetInterfaceName + SERVICE_SUFFIX;
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

    @Override
    public List<InetSocketAddress> pullService(String targetInterfaceName) {
        String servicePath = SERVICE_PREFIX + "/" + targetInterfaceName + SERVICE_SUFFIX;
        try {
            if (this.client.checkExists().forPath(servicePath) != null) {
                return mapPathToSocketAddr(servicePath);
            } else {
                throw new RuntimeException("no services found");
            }
        } catch (Exception e) {
            log.error("service pulling failed", e);
            throw new RuntimeException("service pulling failed");
        }
    }

    private List<InetSocketAddress> mapPathToSocketAddr(String servicePath) throws Exception {
        List<String> serviceAddresses = this.client.getChildren().forPath(servicePath);
        List<InetSocketAddress> inetSocketAddresses = serviceAddresses.stream()
                .map(s -> s.split(":"))
                .map(s -> new InetSocketAddress(s[0], Integer.parseInt(s[1])))
                .collect(Collectors.toList());
        return inetSocketAddresses;
    }

    @Override
    public void subscribeService(String targetInterfaceName, List<InetSocketAddress> existingHostAndPort) {
        String servicePath = SERVICE_PREFIX + "/" + targetInterfaceName + SERVICE_SUFFIX;
        CuratorCache curatorCache = CuratorCache.build(client, servicePath);
        CuratorCacheListener curatorCacheListener = CuratorCacheListener.builder().forPathChildrenCache(servicePath, client, new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                //1 remove all existing addresses
                existingHostAndPort.clear();
                //2 update addresses
                existingHostAndPort.addAll(mapPathToSocketAddr(servicePath));
            }
        }).build();

        curatorCache.listenable().addListener(curatorCacheListener);
        curatorCache.start();
    }
}
