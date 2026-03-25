package com.skuri.skuri_backend.domain.campus.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

@Component
public class CampusBannerOrderLock {

    // Empty-table create cannot rely on row locks, so serialize order-changing operations locally.
    private final ReentrantLock lock = new ReentrantLock();

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
