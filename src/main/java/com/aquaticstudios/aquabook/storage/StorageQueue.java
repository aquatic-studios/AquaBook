package com.aquaticstudios.aquabook.storage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class StorageQueue implements Runnable {
    private final Queue<Runnable> handlingQueue = new ArrayDeque<>();
    private final Thread thread;
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    private volatile boolean kill = false;

    public StorageQueue(String name) {
        this.thread = new Thread(this, name);
        this.thread.setDaemon(true);
        this.thread.start();
    }

    public int pending() {
        lock.lock();
        try {
            return handlingQueue.size();
        } finally {
            lock.unlock();
        }
    }

    public void submit(Runnable runnable) {
        lock.lock();
        try {
            handlingQueue.offer(runnable);
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    public <T> CompletableFuture<T> submitAsync(Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        submit(() -> {
            try {
                future.complete(callable.call());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    public void kill() {
        this.kill = true;
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void shutdown(long millis) {
        kill();
        try {
            thread.join(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        List<Runnable> batch = new ArrayList<>();

        while (!kill) {
            try {
                drainTo(batch);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
            runAll(batch);
            batch.clear();
        }

        List<Runnable> remaining = new ArrayList<>();
        lock.lock();
        try {
            while (!handlingQueue.isEmpty()) remaining.add(handlingQueue.remove());
        } finally {
            lock.unlock();
        }
        runAll(remaining);
    }

    private void runAll(List<Runnable> tasks) {
        for (Runnable task : tasks) {
            try {
                task.run();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private void drainTo(List<Runnable> target) throws InterruptedException {
        lock.lock();
        try {
            while (handlingQueue.isEmpty() && !kill) {
                condition.await();
            }
            while (!handlingQueue.isEmpty()) {
                target.add(handlingQueue.remove());
            }
        } finally {
            lock.unlock();
        }
    }
}
