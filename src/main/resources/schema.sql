-- Database Schema for PayrollX AI

-- Drop tables if they exist (for clean initialization)
DROP TABLE IF EXISTS employee_projects;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS performance;
DROP TABLE IF EXISTS tax_records;
DROP TABLE IF EXISTS deductions;
DROP TABLE IF EXISTS bonuses;
DROP TABLE IF EXISTS salary_history;
DROP TABLE IF EXISTS payroll;
DROP TABLE IF EXISTS leave_requests;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;

-- 1. Roles Table
CREATE TABLE roles (
    id INT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

-- 2. Users Table
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- 3. Departments Table
CREATE TABLE departments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dept_name VARCHAR(100) NOT NULL UNIQUE,
    manager_id INT NULL, -- Simple INT mapping to avoid circular FK references at database layer
    budget DOUBLE NOT NULL DEFAULT 0.0
);

-- 4. Employees Table
CREATE TABLE employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NULL,
    hire_date DATE NOT NULL,
    department_id INT NULL,
    manager_id INT NULL, -- Maps to manager's employee_id
    position VARCHAR(100) NOT NULL,
    salary DOUBLE NOT NULL DEFAULT 0.0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, TERMINATED, RETIRED, RESIGNED
    bank_account VARCHAR(50) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
);

-- Add Foreign Key constraint for department manager (optional database integrity, omitted to bypass initialization ordering issues)

-- 5. Attendance Table
CREATE TABLE attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    attendance_date DATE NOT NULL,
    check_in TIMESTAMP NULL,
    check_out TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL, -- PRESENT, ABSENT, LATE, HALF_DAY, ON_LEAVE
    overtime_hours DOUBLE NOT NULL DEFAULT 0.0,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- 6. Leave Requests Table
CREATE TABLE leave_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    leave_type VARCHAR(50) NOT NULL, -- ANNUAL, SICK, EMERGENCY, MATERNITY, PATERNITY
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    manager_id INT NOT NULL, -- Approving manager employee_id
    comment VARCHAR(255) NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- 7. Payroll Table
CREATE TABLE payroll (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    payroll_month VARCHAR(7) NOT NULL, -- Format: YYYY-MM
    basic_salary DOUBLE NOT NULL DEFAULT 0.0,
    hra DOUBLE NOT NULL DEFAULT 0.0,
    da DOUBLE NOT NULL DEFAULT 0.0,
    bonus DOUBLE NOT NULL DEFAULT 0.0,
    medical_allowance DOUBLE NOT NULL DEFAULT 0.0,
    travel_allowance DOUBLE NOT NULL DEFAULT 0.0,
    overtime_pay DOUBLE NOT NULL DEFAULT 0.0,
    pf DOUBLE NOT NULL DEFAULT 0.0,
    tax DOUBLE NOT NULL DEFAULT 0.0,
    insurance DOUBLE NOT NULL DEFAULT 0.0,
    other_deductions DOUBLE NOT NULL DEFAULT 0.0,
    net_salary DOUBLE NOT NULL DEFAULT 0.0,
    processed_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSED', -- DRAFT, PROCESSED, PAID
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- 8. Salary History Table (Auditing and prediction)
CREATE TABLE salary_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    old_salary DOUBLE NOT NULL,
    new_salary DOUBLE NOT NULL,
    change_date DATE NOT NULL,
    reason VARCHAR(255) NOT NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- 9. Bonuses Table (Individual month adjustments)
CREATE TABLE bonuses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    amount DOUBLE NOT NULL DEFAULT 0.0,
    bonus_type VARCHAR(50) NOT NULL, -- PERFORMANCE, FESTIVAL, REFERRAL
    bonus_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPROVED', -- PENDING, APPROVED, RELEASED
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- 10. Deductions Table (Individual month adjustments)
CREATE TABLE deductions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    amount DOUBLE NOT NULL DEFAULT 0.0,
    deduction_type VARCHAR(50) NOT NULL, -- LOAN, DAMAGE, UNPAID_LEAVE
    deduction_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- 11. Tax Records Table
CREATE TABLE tax_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    financial_year VARCHAR(10) NOT NULL, -- Format: YYYY-YYYY
    taxable_income DOUBLE NOT NULL DEFAULT 0.0,
    tax_paid DOUBLE NOT NULL DEFAULT 0.0,
    deductions_declared DOUBLE NOT NULL DEFAULT 0.0,
    tax_saving_suggestions VARCHAR(500) NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- 12. Performance Table
CREATE TABLE performance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    rating DOUBLE NOT NULL DEFAULT 0.0, -- Scale 1.0 to 5.0
    kpi_score DOUBLE NOT NULL DEFAULT 0.0, -- Scale 0 to 100
    feedback VARCHAR(500) NULL,
    evaluation_date DATE NOT NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- 13. Notifications Table
CREATE TABLE notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    notification_type VARCHAR(20) NOT NULL, -- EMAIL, SMS, IN_APP
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- 14. Audit Logs Table
CREATE TABLE audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL, -- NULL indicates system process action
    action VARCHAR(100) NOT NULL,
    details VARCHAR(500) NOT NULL,
    action_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 15. Projects Table (Workforce allocation)
CREATE TABLE projects (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_name VARCHAR(100) NOT NULL UNIQUE,
    budget DOUBLE NOT NULL DEFAULT 0.0,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' -- IN_PROGRESS, COMPLETED, SUSPENDED
);

-- 16. Employee Projects Table (Many-to-Many mapping)
CREATE TABLE employee_projects (
    employee_id INT NOT NULL,
    project_id INT NOT NULL,
    hours_allocated DOUBLE NOT NULL DEFAULT 0.0,
    PRIMARY KEY (employee_id, project_id),
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Seed Initial System Data (Roles & Default Admin)
INSERT INTO roles (id, role_name) VALUES (1, 'ADMIN');
INSERT INTO roles (id, role_name) VALUES (2, 'HR');
INSERT INTO roles (id, role_name) VALUES (3, 'FINANCE');
INSERT INTO roles (id, role_name) VALUES (4, 'MANAGER');
INSERT INTO roles (id, role_name) VALUES (5, 'EMPLOYEE');

-- Seed a default Admin User (Password will be encrypted, let's seed plaintext/hash 'admin123' -> hash '21232f297a57a5a743894a0e4a801fc3' MD5, or let's use BCrypt/sha256. We'll implement SHA256 in code)
-- sha256 of admin123 is 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
INSERT INTO users (username, password, role_id, status) VALUES ('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 1, 'ACTIVE');
