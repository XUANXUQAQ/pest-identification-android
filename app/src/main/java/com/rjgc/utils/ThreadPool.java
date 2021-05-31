package com.rjgc.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public enum ThreadPool {
    INSTANCE;

    private final ExecutorService threadPool = new ThreadPoolExecutor(0,
            50,
            60L,
            TimeUnit.SECONDS,
            new SynchronousQueue<>());

    /**
     * 线程池中执行线程
     * @param run run
     */
    public void execute(Runnable run) {
        threadPool.execute(run);
    }

    public void shutdown() {
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
