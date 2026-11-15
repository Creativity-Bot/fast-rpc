package com.fastrpc.circuitbreaker.impl;

import com.fastrpc.circuitbreaker.CircuitBreaker;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class CircuitBreakerImpl implements CircuitBreaker {

    /*
    *  key: ip:port
    *  value: State
    * */
    private final Map<String, CountBasedSlidingWindow> circuitBreakers = new ConcurrentHashMap<>();


    @Override
    public boolean allowRequest(InetSocketAddress address) {
        String addressName = address.getAddress().getHostAddress() + ":" + address.getPort();
        CountBasedSlidingWindow slidingWindow = circuitBreakers.computeIfAbsent(addressName, adder -> new CountBasedSlidingWindow(16));
        byte status = slidingWindow.getStatus();
        return status == CountBasedSlidingWindow.CLOSE
                || (status == CountBasedSlidingWindow.OPEN && System.nanoTime() >= slidingWindow.nextRetryNanoTime);
    }

    @Override
    public void update(InetSocketAddress address, boolean isSuccess, Permit permit) {
        String addressName = address.getAddress().getHostAddress() + ":" + address.getPort();
        CountBasedSlidingWindow circuitBreaker = circuitBreakers.computeIfAbsent(addressName, adder -> new CountBasedSlidingWindow(16));
        if(isSuccess){
            circuitBreaker.trySuccess(permit);
        }else{
            circuitBreaker.tryFail(permit);
        }
    }

    @Override
    public Permit tryAcquire(InetSocketAddress address) {
        String addressName = address.getAddress().getHostAddress() + ":" + address.getPort();
        CountBasedSlidingWindow circuitBreaker = circuitBreakers.computeIfAbsent(addressName, adder -> new CountBasedSlidingWindow(16));
        return circuitBreaker.tryAcquire();
    }

    @Data
    @AllArgsConstructor
    public static final class PermitImpl implements Permit{
        private int epoch;
    }



    @Data
    private static final class CountBasedSlidingWindow {

        private static final byte OPEN = (byte)1;
        private static final byte CLOSE = (byte)0;
        private static final byte HALF_OPEN = (byte)2;
        private static final int COOLING_PERIOD = 10;
        private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;


        private final int windowSize; // 2^n to make calc faster
        private byte[] slidingWindow;
        private int totalFailCount;
        private int totalCount;
        private int nextIndex;
        private boolean inFlight;
        private volatile byte status;
        private volatile long nextRetryNanoTime;
        private volatile int epoch;


        public CountBasedSlidingWindow(int windowSize) {
            this.windowSize = windowSize;
            this.slidingWindow = new byte[windowSize]; // 0 means success, 1 means failure
            this.totalFailCount = 0;
            this.totalCount = 0;
            this.nextIndex = 0;
            this.status = CLOSE;
            this.inFlight = false;
            this.nextRetryNanoTime = Long.MAX_VALUE;
            this.epoch = 0;
        }

        public synchronized Permit tryAcquire() {
            if (status == CountBasedSlidingWindow.CLOSE) {
                return new PermitImpl(getEpoch());
            } else if (status == CountBasedSlidingWindow.OPEN && System.nanoTime() >= nextRetryNanoTime && !inFlight) {
                epoch++;
                inFlight = true;
                status = CountBasedSlidingWindow.HALF_OPEN;
                return new PermitImpl(getEpoch());
            } else {
                return null;
            }
        }

        public synchronized void trySuccess(Permit permit){
            PermitImpl p = (PermitImpl) permit;
            if(p.epoch == getEpoch()){
                // 3. if status is half open and update to close
                if (status == CountBasedSlidingWindow.HALF_OPEN) {
                    epoch++;
                    reset();
                } else {
                    // CLOSED
                    totalCount = Math.min(totalCount + 1, windowSize);
                    totalFailCount = totalFailCount - slidingWindow[nextIndex];
                    slidingWindow[nextIndex++] = (byte) 0;
                    nextIndex = nextIndex & (windowSize - 1);
                }
            }
        }

        public synchronized void tryFail(Permit permit){
            PermitImpl p = (PermitImpl) permit;
            if (p.epoch == getEpoch()) {

                totalCount = Math.min(totalCount + 1, windowSize);
                totalFailCount = totalFailCount - slidingWindow[nextIndex] + 1;
                slidingWindow[nextIndex++] = (byte)1;
                nextIndex = nextIndex & (windowSize-1);

                if (status == CountBasedSlidingWindow.HALF_OPEN) {
                    inFlight = false;
                    nextRetryNanoTime = System.nanoTime() +
                            CountBasedSlidingWindow.TIME_UNIT.toNanos(CountBasedSlidingWindow.COOLING_PERIOD);
                    epoch++;
                    status = CountBasedSlidingWindow.OPEN;
                } else {
                    // CLOSED
                    if (totalFailCount > windowSize/2 && (double) totalFailCount / totalCount > 0.5) {
                        inFlight = false;
                        nextRetryNanoTime = System.nanoTime() +
                                CountBasedSlidingWindow.TIME_UNIT.toNanos(CountBasedSlidingWindow.COOLING_PERIOD);
                        epoch++;
                        status = CountBasedSlidingWindow.OPEN;
                    }
                }
            }
        }

        private void reset(){
            this.totalFailCount = 0;
            this.totalCount = 0;
            this.nextIndex = 0;
            this.slidingWindow = new byte[windowSize];
            this.inFlight = false;
            this.nextRetryNanoTime = Long.MAX_VALUE;
            this.status = CountBasedSlidingWindow.CLOSE; // status needs to init in the last place to make sure "happen before"
        }


    }

}
