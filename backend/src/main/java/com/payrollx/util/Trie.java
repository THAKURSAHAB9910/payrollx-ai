package com.payrollx.util;

import java.util.*;

/**
 * Custom Trie implementation for employee name auto-complete and search suggestions.
 */
public class Trie {
    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord = false;
        // Keep track of employee IDs associated with this name (in case of duplicate names)
        List<Integer> employeeIds = new ArrayList<>();
    }

    private final TrieNode root;

    public Trie() {
        this.root = new TrieNode();
    }

    /**
     * Inserts an employee name and their ID into the Trie.
     */
    public void insert(String name, int employeeId) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        String normalized = name.toLowerCase().trim();
        TrieNode current = root;
        for (char ch : normalized.toCharArray()) {
            current = current.children.computeIfAbsent(ch, k -> new TrieNode());
        }
        current.isEndOfWord = true;
        if (!current.employeeIds.contains(employeeId)) {
            current.employeeIds.add(employeeId);
        }
    }

    /**
     * Search suggestions based on prefix.
     * Returns a map of Name -> List of Employee IDs.
     */
    public Map<String, List<Integer>> getSuggestions(String prefix) {
        Map<String, List<Integer>> suggestions = new LinkedHashMap<>();
        if (prefix == null || prefix.trim().isEmpty()) {
            return suggestions;
        }

        String normalized = prefix.toLowerCase().trim();
        TrieNode current = root;

        // Traverse to the end of the prefix
        for (char ch : normalized.toCharArray()) {
            current = current.children.get(ch);
            if (current == null) {
                return suggestions; // No match found
            }
        }

        // Run DFS to collect all words starting with this prefix
        dfsCollect(current, new StringBuilder(normalized), suggestions);
        return suggestions;
    }

    private void dfsCollect(TrieNode node, StringBuilder currentWord, Map<String, List<Integer>> results) {
        if (node.isEndOfWord) {
            results.put(currentWord.toString(), new ArrayList<>(node.employeeIds));
        }

        // Sort keys to provide alphabetical order
        List<Character> sortedKeys = new ArrayList<>(node.children.keySet());
        Collections.sort(sortedKeys);

        for (char ch : sortedKeys) {
            currentWord.append(ch);
            dfsCollect(node.children.get(ch), currentWord, results);
            currentWord.deleteCharAt(currentWord.length() - 1); // backtrack
        }
    }
}
