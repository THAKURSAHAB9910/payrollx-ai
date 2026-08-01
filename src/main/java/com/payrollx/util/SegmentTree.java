package com.payrollx.util;

/**
 * Custom Segment Tree implementation to run Range Queries on employee salaries.
 * Supports range sum (total salary cost) and range max (highest salary) queries in O(log N).
 */
public class SegmentTree {
    private static class SegmentNode {
        double sum;
        double max;
        int start, end;
        SegmentNode left, right;

        SegmentNode(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private final SegmentNode root;
    private final int size;

    public SegmentTree(double[] salaries) {
        if (salaries == null || salaries.length == 0) {
            this.size = 0;
            this.root = null;
            return;
        }
        this.size = salaries.length;
        this.root = buildTree(salaries, 0, salaries.length - 1);
    }

    private SegmentNode buildTree(double[] arr, int start, int end) {
        SegmentNode node = new SegmentNode(start, end);
        if (start == end) {
            node.sum = arr[start];
            node.max = arr[start];
            return node;
        }

        int mid = start + (end - start) / 2;
        node.left = buildTree(arr, start, mid);
        node.right = buildTree(arr, mid + 1, end);

        node.sum = node.left.sum + node.right.sum;
        node.max = Math.max(node.left.max, node.right.max);
        return node;
    }

    /**
     * Updates the value at a specific index in the array and recalculates tree values in O(log N).
     */
    public void update(int index, double val) {
        if (root == null || index < 0 || index >= size) {
            return;
        }
        updateHelper(root, index, val);
    }

    private void updateHelper(SegmentNode node, int index, double val) {
        if (node.start == node.end) {
            node.sum = val;
            node.max = val;
            return;
        }

        int mid = node.start + (node.end - node.start) / 2;
        if (index <= mid) {
            updateHelper(node.left, index, val);
        } else {
            updateHelper(node.right, index, val);
        }

        node.sum = node.left.sum + node.right.sum;
        node.max = Math.max(node.left.max, node.right.max);
    }

    /**
     * Queries the sum of salaries in the range [left, right] in O(log N).
     */
    public double querySum(int left, int right) {
        if (root == null || left > right || left < 0 || right >= size) {
            return 0.0;
        }
        return querySumHelper(root, left, right);
    }

    private double querySumHelper(SegmentNode node, int left, int right) {
        if (node.start >= left && node.end <= right) {
            return node.sum;
        }
        if (node.end < left || node.start > right) {
            return 0.0;
        }

        return querySumHelper(node.left, left, right) + querySumHelper(node.right, left, right);
    }

    /**
     * Queries the max salary in the range [left, right] in O(log N).
     */
    public double queryMax(int left, int right) {
        if (root == null || left > right || left < 0 || right >= size) {
            return 0.0;
        }
        return queryMaxHelper(root, left, right);
    }

    private double queryMaxHelper(SegmentNode node, int left, int right) {
        if (node.start >= left && node.end <= right) {
            return node.max;
        }
        if (node.end < left || node.start > right) {
            return Double.NEGATIVE_INFINITY;
        }

        return Math.max(queryMaxHelper(node.left, left, right), queryMaxHelper(node.right, left, right));
    }
}
