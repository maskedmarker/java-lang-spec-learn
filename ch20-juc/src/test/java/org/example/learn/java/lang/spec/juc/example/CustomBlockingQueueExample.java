package org.example.learn.java.lang.spec.juc.example;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Steps to Implement a Custom Blocking Queue
 *  1. Choose a base implementation
 *          Decide on the backing data structure
 *          Implement thread-safe operations using synchronization primitives, such as synchronized, ReentrantLock, or higher-level concurrency tools like Condition.
 *  2. Implement blocking behavior
 *          Threads should block if the queue is full (on put()).
 *          Threads should block if the queue is empty (on take()).
 *  3. Implement synchronization
 *          Use wait() and notifyAll() (or Condition objects) to manage thread signaling between producers and consumers.
 *  4. Handle boundary conditions
 *          Implement additional logic for managing maximum capacity, null elements (optional), or custom priorities.
 *
 *
 * @param <T>
 */
public class CustomBlockingQueueExample<T> {

    private final T[] elements;
    private int head = 0;  // Points to the oldest element
    private int tail = 0;  // Points to the next insertion point
    private int count = 0; // Number of elements in the queue

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    public CustomBlockingQueueExample(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("Queue capacity must be greater than 0.");
        elements = (T[]) new Object[capacity];
    }

    // Add an element to the queue (blocks if full)
    public void put(T element) throws InterruptedException {
        if (element == null) throw new NullPointerException("Null elements are not allowed.");
        lock.lock();
        try {

            // notice blocking condition
            while (count == elements.length) {
                notFull.await(); // Wait until there is space
            }

            elements[tail] = element;
            tail = (tail + 1) % elements.length; // Circular buffer logic
            count++;
            notEmpty.signal(); // Notify a waiting consumer
        } finally {
            lock.unlock();
        }
    }

    // Retrieve and remove the head of the queue (blocks if empty)
    public T take() throws InterruptedException {
        lock.lock();
        try {

            // notice blocking condition
            while (count == 0) {
                notEmpty.await(); // Wait until there is something to consume
            }

            T element = elements[head];
            elements[head] = null; // Remove the element
            head = (head + 1) % elements.length; // Circular buffer logic
            count--;
            notFull.signal(); // Notify a waiting producer
            return element;
        } finally {
            lock.unlock();
        }
    }

    // Return the current number of elements in the queue
    public int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    // Return the capacity of the queue
    public int capacity() {
        return elements.length;
    }
}
