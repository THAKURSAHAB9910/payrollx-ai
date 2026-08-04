package com.payrollx.util;

import java.util.HashMap;
import java.util.Map;

/**
 * A custom implementation of a generic Least Recently Used (LRU) Cache.
 * Used for O(1) retrieval of frequently accessed database objects like Employee records.
 */
public class LruCache<K, V> {
    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> map;
    private Node<K, V> head;
    private Node<K, V> tail;

    public LruCache(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>();
    }

    /**
     * Retrieves a value from the cache. Moves accessed node to the head (most recently used).
     */
    public synchronized V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) {
            return null;
        }
        moveToHead(node);
        return node.value;
    }

    /**
     * Inserts or updates a value in the cache. Evicts the least recently used element if capacity is exceeded.
     */
    public synchronized void put(K key, V value) {
        Node<K, V> node = map.get(key);
        if (node != null) {
            node.value = value;
            moveToHead(node);
        } else {
            Node<K, V> newNode = new Node<>(key, value);
            map.put(key, newNode);
            addToHead(newNode);
            if (map.size() > capacity) {
                Node<K, V> lruNode = removeTail();
                if (lruNode != null) {
                    map.remove(lruNode.key);
                }
            }
        }
    }

    /**
     * Removes an item from the cache.
     */
    public synchronized void remove(K key) {
        Node<K, V> node = map.remove(key);
        if (node != null) {
            removeNode(node);
        }
    }

    /**
     * Clears all items in the cache.
     */
    public synchronized void clear() {
        map.clear();
        head = null;
        tail = null;
    }

    public synchronized int size() {
        return map.size();
    }

    // Helper functions for Doubly Linked List manipulation

    private void addToHead(Node<K, V> node) {
        if (head == null) {
            head = node;
            tail = node;
        } else {
            node.next = head;
            head.prev = node;
            head = node;
        }
    }

    private void removeNode(Node<K, V> node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }

        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
        }

        node.prev = null;
        node.next = null;
    }

    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addToHead(node);
    }

    private Node<K, V> removeTail() {
        if (tail == null) {
            return null;
        }
        Node<K, V> oldTail = tail;
        removeNode(tail);
        return oldTail;
    }
}
