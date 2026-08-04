package com.payrollx.model;

import java.time.LocalDate;

public class Employee {
    private int id;
    private int userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate hireDate;
    private Integer departmentId;
    private Integer managerId;
    private String position;
    private double salary;
    private String status;
    private String bankAccount;

    // Private constructor, only accessible by Builder
    private Employee(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.email = builder.email;
        this.phone = builder.phone;
        this.hireDate = builder.hireDate;
        this.departmentId = builder.departmentId;
        this.managerId = builder.managerId;
        this.position = builder.position;
        this.salary = builder.salary;
        this.status = builder.status;
        this.bankAccount = builder.bankAccount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }

    public Integer getManagerId() { return managerId; }
    public void setManagerId(Integer managerId) { this.managerId = managerId; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Builder Class for Employee.
     */
    public static class Builder {
        private int id;
        private int userId;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private LocalDate hireDate;
        private Integer departmentId;
        private Integer managerId;
        private String position;
        private double salary;
        private String status = "ACTIVE";
        private String bankAccount;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder userId(int userId) {
            this.userId = userId;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder hireDate(LocalDate hireDate) {
            this.hireDate = hireDate;
            return this;
        }

        public Builder departmentId(Integer departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        public Builder managerId(Integer managerId) {
            this.managerId = managerId;
            return this;
        }

        public Builder position(String position) {
            this.position = position;
            return this;
        }

        public Builder salary(double salary) {
            this.salary = salary;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder bankAccount(String bankAccount) {
            this.bankAccount = bankAccount;
            return this;
        }

        public Employee build() {
            if (firstName == null || lastName == null || email == null || bankAccount == null) {
                throw new IllegalStateException("Required fields (firstName, lastName, email, bankAccount) are missing.");
            }
            return new Employee(this);
        }
    }
}
