package com.payrollx.util;

import java.util.*;

/**
 * Custom Directed Graph representing manager-report relationships and department hierarchies.
 */
public class OrgGraph {
    // Adjacency List: Manager ID -> Set of Direct Report IDs
    private final Map<Integer, Set<Integer>> adjList;
    // Map to quickly find the manager of any employee (Reversed Edge)
    private final Map<Integer, Integer> employeeToManager;

    public OrgGraph() {
        this.adjList = new HashMap<>();
        this.employeeToManager = new HashMap<>();
    }

    /**
     * Adds an employee-manager edge.
     */
    public void addRelationship(int employeeId, Integer managerId) {
        if (managerId == null || managerId == 0) {
            employeeToManager.remove(employeeId);
            return;
        }

        // Avoid self-loop or cyclic dependencies
        if (employeeId == managerId) {
            throw new IllegalArgumentException("An employee cannot be their own manager.");
        }

        employeeToManager.put(employeeId, managerId);
        adjList.computeIfAbsent(managerId, k -> new HashSet<>()).add(employeeId);
    }

    /**
     * Returns the direct reports of an employee.
     */
    public Set<Integer> getDirectReports(int managerId) {
        return adjList.getOrDefault(managerId, Collections.emptySet());
    }

    /**
     * Traverses the organization chart using BFS starting from a manager.
     * Returns a list of all subordinates level-by-level.
     */
    public List<Integer> getSubordinatesBFS(int managerId) {
        List<Integer> result = new ArrayList<>();
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        queue.add(managerId);
        visited.add(managerId);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            // Don't include the manager themselves in their subordinates list
            if (current != managerId) {
                result.add(current);
            }

            Set<Integer> directReports = adjList.getOrDefault(current, Collections.emptySet());
            for (int report : directReports) {
                if (!visited.contains(report)) {
                    visited.add(report);
                    queue.add(report);
                }
            }
        }
        return result;
    }

    /**
     * Traverses the organization chart using DFS starting from a manager.
     * Returns a list of all subordinates in depth-first order.
     */
    public List<Integer> getSubordinatesDFS(int managerId) {
        List<Integer> result = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        dfsHelper(managerId, visited, result);
        // Remove the starting manager from the list of reports
        if (!result.isEmpty()) {
            result.remove(0);
        }
        return result;
    }

    private void dfsHelper(int current, Set<Integer> visited, List<Integer> result) {
        visited.add(current);
        result.add(current);

        Set<Integer> directReports = adjList.getOrDefault(current, Collections.emptySet());
        for (int report : directReports) {
            if (!visited.contains(report)) {
                dfsHelper(report, visited, result);
            }
        }
    }

    /**
     * Gets the reporting chain (escalation path) from employee to the top level.
     * e.g., Employee -> Manager -> Grand Manager -> CEO
     */
    public List<Integer> getReportingChain(int employeeId) {
        List<Integer> chain = new ArrayList<>();
        Integer current = employeeId;
        while (current != null) {
            chain.add(current);
            current = employeeToManager.get(current);
        }
        return chain;
    }

    /**
     * Checks if there's a cycle if employeeId reports to newManagerId.
     */
    public boolean wouldIntroduceCycle(int employeeId, int newManagerId) {
        if (employeeId == newManagerId) {
            return true;
        }
        Integer current = newManagerId;
        while (current != null) {
            if (current == employeeId) {
                return true;
            }
            current = employeeToManager.get(current);
        }
        return false;
    }

    /**
     * Clears all entries in the graph.
     */
    public void clear() {
        adjList.clear();
        employeeToManager.clear();
    }
}
