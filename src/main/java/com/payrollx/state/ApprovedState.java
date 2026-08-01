package com.payrollx.state;

import com.payrollx.model.LeaveRequest;

/**
 * Concrete Approved State for Leave Request workflow.
 */
public class ApprovedState implements LeaveState {

    @Override
    public void approve(LeaveRequest request, int managerId, String comment) {
        throw new IllegalStateException("Leave request is already approved.");
    }

    @Override
    public void reject(LeaveRequest request, int managerId, String comment) {
        throw new IllegalStateException("Approved leave request cannot be rejected.");
    }

    @Override
    public String getStatusName() {
        return "APPROVED";
    }
}
