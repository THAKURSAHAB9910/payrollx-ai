package com.payrollx.state;

import com.payrollx.model.LeaveRequest;

/**
 * Concrete Pending State for Leave Request workflow.
 */
public class PendingState implements LeaveState {

    @Override
    public void approve(LeaveRequest request, int managerId, String comment) {
        request.setStatus("APPROVED");
        request.setManagerId(managerId);
        request.setComment(comment);
    }

    @Override
    public void reject(LeaveRequest request, int managerId, String comment) {
        request.setStatus("REJECTED");
        request.setManagerId(managerId);
        request.setComment(comment);
    }

    @Override
    public String getStatusName() {
        return "PENDING";
    }
}
