package com.payrollx.util;

/**
 * Custom Stack implementation using a singly linked list.
 * Used for storing administrative action commands to support undo operations.
 */
public class CommandStack<T> {
    private static class Node<T> {
        T data;
        Node<T> next;

        Node(T data) {
            this.data = data;
        }
    }

    private Node<T> top;
    private int size;

    public CommandStack() {
        this.top = null;
        this.size = 0;
    }

    /**
     * Pushes a new element onto the stack.
     */
    public void push(T data) {
        Node<T> newNode = new Node<>(data);
        newNode.next = top;
        top = newNode;
        size++;
    }

    /**
     * Pops and returns the top element of the stack.
     * Returns null if the stack is empty.
     */
    public T pop() {
        if (isEmpty()) {
            return null;
        }
        T data = top.data;
        top = top.next;
        size--;
        return data;
    }

    /**
     * Peeks the top element without removing it.
     */
    public T peek() {
        if (isEmpty()) {
            return null;
        }
        return top.data;
    }

    public boolean isEmpty() {
        return top == null;
    }

    public int size() {
        return size;
    }

    public void clear() {
        top = null;
        size = 0;
    }
}
