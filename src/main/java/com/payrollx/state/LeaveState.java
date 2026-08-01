package com.payrollx.state;

import com.payrollx.model.LeaveRequest;

/**
 * State interface for the State Design Pattern.
 * Manages transitions of LeaveRequest approvals.
 */
public interface LeaveState {
    void approve(LeaveRequest request, int managerId, String comment);
    void reject(LeaveRequest request, int managerId, String comment);
    String getStatusName();
}
