package com.payrollx.model;

public enum RoleType {
    ADMIN(1),
    HR(2),
    FINANCE(3),
    MANAGER(4),
    EMPLOYEE(5);

    private final int id;

    RoleType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static RoleType fromId(int id) {
        for (RoleType type : RoleType.values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return EMPLOYEE;
    }
}
