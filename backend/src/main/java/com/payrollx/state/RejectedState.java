package com.payrollx.state;

import com.payrollx.model.LeaveRequest;

/**
 * Concrete Rejected State for Leave Request workflow.
 */
public class RejectedState implements LeaveState {

    @Override
    public void approve(LeaveRequest request, int managerId, String comment) {
        throw new IllegalStateException("Rejected leave request cannot be approved.");
    }

    @Override
    public void reject(LeaveRequest request, int managerId, String comment) {
        throw new IllegalStateException("Leave request is already rejected.");
    }

    @Override
    public String getStatusName() {
        return "REJECTED";
    }
}
