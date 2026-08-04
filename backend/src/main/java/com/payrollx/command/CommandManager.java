package com.payrollx.command;

import com.payrollx.util.CommandStack;

/**
 * Invoker class in the Command Pattern.
 * Manages the command execution and undo stack using the custom CommandStack utility.
 */
public class CommandManager {
    private static CommandManager instance;
    private final CommandStack<Command> undoStack;

    private CommandManager() {
        this.undoStack = new CommandStack<>();
    }

    public static synchronized CommandManager getInstance() {
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }

    /**
     * Executes a command and pushes it to the undo stack.
     */
    public synchronized boolean executeCommand(Command cmd) {
        if (cmd.execute()) {
            undoStack.push(cmd);
            return true;
        }
        return false;
    }

    /**
     * Undoes the last executed command.
     */
    public synchronized boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        Command cmd = undoStack.pop();
        return cmd.undo();
    }

    public synchronized String getLastCommandDescription() {
        if (undoStack.isEmpty()) {
            return "No commands to undo";
        }
        return undoStack.peek().getDescription();
    }

    public synchronized int getUndoStackSize() {
        return undoStack.size();
    }

    public synchronized void clearHistory() {
        undoStack.clear();
    }
}
