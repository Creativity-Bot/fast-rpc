package com.fastrpc.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fastrpc")
public class FrpcProperties {

    private final Registry registry = new Registry();

    private final Server server = new Server();

    private final RateLimit rateLimit = new RateLimit();

    public Registry getRegistry() {
        return registry;
    }

    public Server getServer() {
        return server;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public static class Registry {

        private String address = "127.0.0.1:2181";

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }

    public static class Server {

        private boolean enabled = false;

        private String ip = "localhost";

        private int port = 9527;

        private int workerThreads = 4;

        private int handlerThreads = 16;

        private int serviceThreads = 16;

        private long readIdleSeconds = 15;

        private long writeIdleSeconds = 0;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getWorkerThreads() {
            return workerThreads;
        }

        public void setWorkerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
        }

        public int getHandlerThreads() {
            return handlerThreads;
        }

        public void setHandlerThreads(int handlerThreads) {
            this.handlerThreads = handlerThreads;
        }

        public int getServiceThreads() {
            return serviceThreads;
        }

        public void setServiceThreads(int serviceThreads) {
            this.serviceThreads = serviceThreads;
        }

        public long getReadIdleSeconds() {
            return readIdleSeconds;
        }

        public void setReadIdleSeconds(long readIdleSeconds) {
            this.readIdleSeconds = readIdleSeconds;
        }

        public long getWriteIdleSeconds() {
            return writeIdleSeconds;
        }

        public void setWriteIdleSeconds(long writeIdleSeconds) {
            this.writeIdleSeconds = writeIdleSeconds;
        }
    }

    public static class RateLimit {

        private double permitsPerSecond = 100;

        private double capacity = 100;

        public double getPermitsPerSecond() {
            return permitsPerSecond;
        }

        public void setPermitsPerSecond(double permitsPerSecond) {
            this.permitsPerSecond = permitsPerSecond;
        }

        public double getCapacity() {
            return capacity;
        }

        public void setCapacity(double capacity) {
            this.capacity = capacity;
        }
    }
}
