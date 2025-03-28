package org.chubby.github.pebblescrate.common.lootcrates;

import io.netty.util.concurrent.SingleThreadEventExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        try {
            executor.getQueue().offer(r, 10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Log the exception or handle it appropriately
        }
    }
}
