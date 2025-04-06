package org.chubby.github.pebblescrate.common.lootcrates;

import io.netty.util.concurrent.SingleThreadEventExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        try {
            // Try for a shorter time to avoid blocking
            boolean offered = executor.getQueue().offer(r, 2, TimeUnit.SECONDS);
            if (!offered) {
                System.err.println("Task rejected from executor - queue full or timeout reached");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread interrupted while waiting to requeue task");
        }
    }
}