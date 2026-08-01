package com.payrollx.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Custom implementations of Quick Sort and Merge Sort for lists of objects.
 * Demonstrates advanced sorting logic using custom Comparators.
 */
public class Sorter {

    /**
     * Sorts the list using a custom Merge Sort implementation.
     */
    public static <T> void mergeSort(List<T> list, Comparator<? super T> comparator) {
        if (list == null || list.size() <= 1) {
            return;
        }
        List<T> sorted = mergeSortHelper(list, comparator);
        for (int i = 0; i < list.size(); i++) {
            list.set(i, sorted.get(i));
        }
    }

    private static <T> List<T> mergeSortHelper(List<T> list, Comparator<? super T> comparator) {
        if (list.size() <= 1) {
            return list;
        }

        int mid = list.size() / 2;
        List<T> left = new ArrayList<>(list.subList(0, mid));
        List<T> right = new ArrayList<>(list.subList(mid, list.size()));

        left = mergeSortHelper(left, comparator);
        right = mergeSortHelper(right, comparator);

        return merge(left, right, comparator);
    }

    private static <T> List<T> merge(List<T> left, List<T> right, Comparator<? super T> comparator) {
        List<T> merged = new ArrayList<>();
        int i = 0, j = 0;

        while (i < left.size() && j < right.size()) {
            if (comparator.compare(left.get(i), right.get(j)) <= 0) {
                merged.add(left.get(i));
                i++;
            } else {
                merged.add(right.get(j));
                j++;
            }
        }

        while (i < left.size()) {
            merged.add(left.get(i));
            i++;
        }

        while (j < right.size()) {
            merged.add(right.get(j));
            j++;
        }

        return merged;
    }

    /**
     * Sorts the list using a custom Quick Sort implementation.
     */
    public static <T> void quickSort(List<T> list, Comparator<? super T> comparator) {
        if (list == null || list.size() <= 1) {
            return;
        }
        quickSortHelper(list, 0, list.size() - 1, comparator);
    }

    private static <T> void quickSortHelper(List<T> list, int low, int high, Comparator<? super T> comparator) {
        if (low < high) {
            int pivotIndex = partition(list, low, high, comparator);
            quickSortHelper(list, low, pivotIndex - 1, comparator);
            quickSortHelper(list, pivotIndex + 1, high, comparator);
        }
    }

    private static <T> int partition(List<T> list, int low, int high, Comparator<? super T> comparator) {
        T pivot = list.get(high);
        int i = low - 1;

        for (int j = low; j < high; j++) {
            if (comparator.compare(list.get(j), pivot) <= 0) {
                i++;
                swap(list, i, j);
            }
        }

        swap(list, i + 1, high);
        return i + 1;
    }

    private static <T> void swap(List<T> list, int i, int j) {
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }
}
