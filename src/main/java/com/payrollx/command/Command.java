package com.payrollx.command;

/**
 * Command interface for the Command Design Pattern.
 * Supports execution and undo operations for administrative actions.
 */
public interface Command {
    boolean execute();
    boolean undo();
    String getDescription();
}
