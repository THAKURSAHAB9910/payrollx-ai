package com.payrollx;

import com.payrollx.config.DatabaseConnectionManager;
import com.payrollx.command.*;
import com.payrollx.dao.*;
import com.payrollx.factory.EmployeeFactory;
import com.payrollx.factory.ReportFactory;
import com.payrollx.model.*;
import com.payrollx.observer.*;
import com.payrollx.service.*;
import com.payrollx.state.PendingState;
import com.payrollx.template.ReportGenerator;
import com.payrollx.util.SecurityUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    
    private static final UserDao USER_DAO = new UserDao();
    private static final EmployeeDao EMPLOYEE_DAO = new EmployeeDao();
    private static final DepartmentDao DEPARTMENT_DAO = new DepartmentDao();
    private static final AttendanceDao ATTENDANCE_DAO = new AttendanceDao();
    private static final LeaveRequestDao LEAVE_REQUEST_DAO = new LeaveRequestDao();
    private static final PayrollDao PAYROLL_DAO = new PayrollDao();
    private static final AuditLogDao AUDIT_LOG_DAO = new AuditLogDao();
    private static final ProjectDao PROJECT_DAO = new ProjectDao();
    private static final PerformanceDao PERFORMANCE_DAO = new PerformanceDao();

    private static final AIService AI_SERVICE = new AIService();
    private static final AnomalyDetectionService ANOMALY_SERVICE = new AnomalyDetectionService();
    private static final WorkforceAnalyticsService ANALYTICS_SERVICE = new WorkforceAnalyticsService();
    private static final AnalyticsService DASHBOARD_SERVICE = new AnalyticsService();

    private static User currentUser = null;
    private static Employee currentEmployee = null;

    public static void main(String[] args) {
        // Register JVM Shutdown Hook for clean resource cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            com.payrollx.controller.HttpServerController.stopServer();
            DatabaseConnectionManager.getInstance().shutdown();
            System.out.println("PayrollX AI services stopped cleanly.");
        }));

        // Initialize Notification system
        NotificationSystem ns = NotificationSystem.getInstance();
        ns.attach(new EmailNotificationListener());
        ns.attach(new SmsNotificationListener());
        ns.attach(new InAppNotificationListener());

        // Initialize Database
        DatabaseConnectionManager.getInstance();

        // Start Embedded Web Server
        com.payrollx.controller.HttpServerController.startServer();
        
        // Seed initial data
        seedInitialData();

        Scanner scanner = new Scanner(System.in);
        System.out.println("========================================================================");
        System.out.println("                 Welcome to PayrollX AI - Enterprise HR                 ");
        System.out.println("========================================================================");

        boolean running = true;
        while (running) {
            if (currentUser == null) {
                showLoginMenu(scanner);
            } else {
                showRoleMenu(scanner);
            }
        }
        
        // Exit notification (resource cleanup managed by shutdown hook)
        System.out.println("CLI Session exited.");
    }

    private static void showLoginMenu(Scanner scanner) {
        System.out.println("\n--- LOGIN SECTION ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Username and password cannot be empty.");
            return;
        }

        User user = USER_DAO.getByUsername(username);
        if (user != null && user.getPassword().equals(SecurityUtils.hashPassword(password))) {
            if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                System.out.println("Your account is currently suspended.");
                return;
            }
            currentUser = user;
            currentEmployee = EMPLOYEE_DAO.getByUserId(user.getId());
            System.out.println("\nSUCCESS: Login successful! Welcome, " + username + " [" + user.getRole() + "]");
            
            // Log Login Audit
            AuditLog log = new AuditLog();
            log.setUserId(user.getId());
            log.setAction("LOGIN");
            log.setDetails("User " + username + " successfully logged in.");
            log.setTimestamp(LocalDateTime.now());
            AUDIT_LOG_DAO.create(log);
        } else {
            System.out.println("ERROR: Invalid credentials. Try again.");
        }
    }

    private static void showRoleMenu(Scanner scanner) {
        RoleType role = currentUser.getRole();
        System.out.println("\n=============================================");
        System.out.println(" ROLE PROFILE: " + role);
        System.out.println("=============================================");
        
        switch (role) {
            case ADMIN:
                showAdminMenu(scanner);
                break;
            case HR:
                showHRMenu(scanner);
                break;
            case FINANCE:
                showFinanceMenu(scanner);
                break;
            case MANAGER:
                showManagerMenu(scanner);
                break;
            case EMPLOYEE:
                showEmployeeMenu(scanner);
                break;
        }
    }

    // ----------------------------------------------------
    // ADMIN ACTIONS
    // ----------------------------------------------------
    private static void showAdminMenu(Scanner scanner) {
        System.out.println("1. Create Department");
        System.out.print("2. Add Full-Time Employee (Factory)\n");
        System.out.println("3. Adjust Employee Salary (Command/Undoable)");
        System.out.println("4. Promote Employee (Command/Undoable)");
        System.out.println("5. Revert Last Administrative Action (Undo)");
        System.out.println("6. Run Payroll Anomaly Checks");
        System.out.println("7. View System Audit Logs");
        System.out.println("8. View Analytics Dashboard");
        System.out.println("9. Logout");
        System.out.print("Select choice: ");
        
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                createDepartment(scanner);
                break;
            case "2":
                addEmployee(scanner);
                break;
            case "3":
                adjustSalary(scanner);
                break;
            case "4":
                promoteEmployee(scanner);
                break;
            case "5":
                revertLastAction();
                break;
            case "6":
                runAnomalies();
                break;
            case "7":
                viewAuditLogs();
                break;
            case "8":
                System.out.println(DASHBOARD_SERVICE.generateDashboardReport());
                break;
            case "9":
                handleLogout();
                break;
            default:
                System.out.println("Invalid choice.");
        }
    }

    private static void createDepartment(Scanner scanner) {
        System.out.print("Department Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Budget ($): ");
        double budget = Double.parseDouble(scanner.nextLine().trim());
        
        Department dept = new Department();
        dept.setDeptName(name);
        dept.setBudget(budget);
        if (DEPARTMENT_DAO.create(dept)) {
            System.out.println("SUCCESS: Department created!");
        } else {
            System.out.println("ERROR: Could not create department.");
        }
    }

    private static void addEmployee(Scanner scanner) {
        System.out.print("First Name: ");
        String fName = scanner.nextLine().trim();
        System.out.print("Last Name: ");
        String lName = scanner.nextLine().trim();
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Phone: ");
        String phone = scanner.nextLine().trim();
        System.out.print("Bank Account: ");
        String bankAcc = scanner.nextLine().trim();
        System.out.print("Base Salary ($): ");
        double salary = Double.parseDouble(scanner.nextLine().trim());
        
        // List departments
        System.out.println("Departments available:");
        for (Department d : DEPARTMENT_DAO.getAll()) {
            System.out.println("  " + d.getId() + ". " + d.getDeptName());
        }
        System.out.print("Select Department ID: ");
        int deptId = Integer.parseInt(scanner.nextLine().trim());

        // Create a matching User Account first
        String username = fName.toLowerCase() + "_" + lName.toLowerCase();
        User user = new User();
        user.setUsername(username);
        user.setPassword(SecurityUtils.hashPassword("password123"));
        user.setRole(RoleType.EMPLOYEE);
        user.setStatus("ACTIVE");
        
        if (USER_DAO.create(user)) {
            Employee emp = EmployeeFactory.createEmployee(
                    EmployeeFactory.EmploymentType.FULL_TIME,
                    fName, lName, email, phone, bankAcc, salary, deptId, null
            );
            emp.setUserId(user.getId());
            if (EMPLOYEE_DAO.create(emp)) {
                System.out.println("SUCCESS: Employee profile and User account (" + username + "/password123) created!");
            } else {
                System.out.println("ERROR: Failed to create employee details.");
            }
        } else {
            System.out.println("ERROR: Failed to create User credentials.");
        }
    }

    private static void adjustSalary(Scanner scanner) {
        System.out.print("Enter Employee ID: ");
        int empId = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter New Salary ($): ");
        double salary = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Reason: ");
        String reason = scanner.nextLine().trim();

        UpdateSalaryCommand cmd = new UpdateSalaryCommand(empId, salary, reason, currentUser.getId());
        if (CommandManager.getInstance().executeCommand(cmd)) {
            System.out.println("SUCCESS: Salary updated. Reversible.");
        } else {
            System.out.println("ERROR: Salary adjustment failed.");
        }
    }

    private static void promoteEmployee(Scanner scanner) {
        System.out.print("Enter Employee ID: ");
        int empId = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter New Position: ");
        String pos = scanner.nextLine().trim();
        System.out.print("Enter New Salary ($): ");
        double salary = Double.parseDouble(scanner.nextLine().trim());

        PromoteEmployeeCommand cmd = new PromoteEmployeeCommand(empId, pos, salary, currentUser.getId());
        if (CommandManager.getInstance().executeCommand(cmd)) {
            System.out.println("SUCCESS: Employee promoted. Reversible.");
        } else {
            System.out.println("ERROR: Promotion action failed.");
        }
    }

    private static void revertLastAction() {
        CommandManager mgr = CommandManager.getInstance();
        if (mgr.getUndoStackSize() == 0) {
            System.out.println("No administrative actions to undo.");
            return;
        }
        String desc = mgr.getLastCommandDescription();
        System.out.println("Attempting to undo: " + desc);
        if (mgr.undo()) {
            System.out.println("SUCCESS: Command successfully reverted.");
        } else {
            System.out.println("ERROR: Reversion failed.");
        }
    }

    private static void runAnomalies() {
        System.out.println("\nRunning Anomaly Detection checks...");
        List<AnomalyDetectionService.Anomaly> list = ANOMALY_SERVICE.runAnomalyChecks();
        if (list.isEmpty()) {
            System.out.println("No anomalies or potential frauds detected in database.");
        } else {
            System.out.println("WARNING: Detected Anomalies!");
            for (AnomalyDetectionService.Anomaly a : list) {
                System.out.println("  " + a);
            }
        }
    }

    private static void viewAuditLogs() {
        System.out.println("\n--- SYSTEM AUDIT LOGS ---");
        List<AuditLog> list = AUDIT_LOG_DAO.getAll();
        for (AuditLog log : list) {
            System.out.printf("[%s] Action: %-15s Details: %s\n", 
                    log.getTimestamp(), log.getAction(), log.getDetails());
        }
    }

    // ----------------------------------------------------
    // HR ACTIONS
    // ----------------------------------------------------
    private static void showHRMenu(Scanner scanner) {
        System.out.println("1. List Employees");
        System.out.println("2. Get Promotion Recommendations (Priority Queue)");
        System.out.println("3. AI HR Assistant Console");
        System.out.println("4. View Analytics Dashboard");
        System.out.println("5. Logout");
        System.out.print("Select choice: ");
        
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                listEmployees();
                break;
            case "2":
                recommendPromotions();
                break;
            case "3":
                runAIAssistant(scanner);
                break;
            case "4":
                System.out.println(DASHBOARD_SERVICE.generateDashboardReport());
                break;
            case "5":
                handleLogout();
                break;
            default:
                System.out.println("Invalid choice.");
        }
    }

    private static void listEmployees() {
        List<Employee> list = EMPLOYEE_DAO.getAll();
        System.out.printf("\n%-5s | %-20s | %-15s | %-12s | %-10s\n", "ID", "Name", "Position", "Status", "Salary");
        System.out.println("------------------------------------------------------------------------");
        for (Employee e : list) {
            System.out.printf("%-5d | %-20s | %-15s | %-12s | $%-9.2f\n", 
                    e.getId(), e.getFullName(), e.getPosition(), e.getStatus(), e.getSalary());
        }
    }

    private static void recommendPromotions() {
        System.out.println("\nComputing promotion candidates based on Rating, Attendance and Tenure...");
        List<WorkforceAnalyticsService.PromotionCandidate> list = ANALYTICS_SERVICE.getPromotionRecommendations();
        if (list.isEmpty()) {
            System.out.println("No candidates computed.");
            return;
        }
        System.out.printf("\n%-5s | %-20s | %-10s | %s\n", "Rank", "Candidate Name", "Score", "Factors Evaluated");
        System.out.println("------------------------------------------------------------------------");
        int rank = 1;
        for (WorkforceAnalyticsService.PromotionCandidate c : list) {
            System.out.printf("%-5d | %-20s | %-10.1f | %s\n", 
                    rank++, c.employee.getFullName(), c.score, c.reasons);
        }
    }

    // ----------------------------------------------------
    // FINANCE ACTIONS
    // ----------------------------------------------------
    private static void showFinanceMenu(Scanner scanner) {
        System.out.println("1. Calculate & Generate Monthly Payroll");
        System.out.println("2. Export Monthly Payroll Report (CSV)");
        System.out.println("3. Predict Next Month's Payroll Liability");
        System.out.println("4. Simulate Salaries");
        System.out.println("5. Logout");
        System.out.print("Select choice: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                calculatePayroll(scanner);
                break;
            case "2":
                exportPayrollReport(scanner);
                break;
            case "3":
                predictPayroll();
                break;
            case "4":
                runFinanceSalarySimulator(scanner);
                break;
            case "5":
                handleLogout();
                break;
            default:
                System.out.println("Invalid choice.");
        }
    }

    private static void calculatePayroll(Scanner scanner) {
        System.out.print("Enter Target Month (YYYY-MM): ");
        String month = scanner.nextLine().trim();

        // Fetch all active employees
        List<Employee> list = EMPLOYEE_DAO.getAll();
        int count = 0;
        for (Employee emp : list) {
            if (!"ACTIVE".equalsIgnoreCase(emp.getStatus())) continue;
            
            // Generate basic salary
            double basic = emp.getSalary();
            
            // Decorator Allowances
            com.payrollx.decorator.SalaryComponent comp = new com.payrollx.decorator.BaseSalary(basic);
            comp = new com.payrollx.decorator.HraDecorator(comp, basic);
            comp = new com.payrollx.decorator.MedicalAllowanceDecorator(comp, 2000.0);
            comp = new com.payrollx.decorator.TravelAllowanceDecorator(comp, 1600.0);

            double gross = comp.getAmount();
            
            // Deductions
            double pf = basic * 0.12;
            
            // Strategy Tax computation (Standard Old regime for default process)
            double annualGross = gross * 12;
            double taxVal = new com.payrollx.strategy.OldRegimeTaxStrategy().calculateTax(annualGross, pf * 12) / 12;
            double insurance = 1000.0;
            
            double net = gross - (pf + taxVal + insurance);

            Payroll p = new Payroll();
            p.setEmployeeId(emp.getId());
            p.setPayrollMonth(month);
            p.setBasicSalary(basic);
            p.setHra(basic * 0.40);
            p.setDa(0.0); // flat DA omitted
            p.setBonus(0.0);
            p.setMedicalAllowance(2000.0);
            p.setTravelAllowance(1600.0);
            p.setOvertimePay(0.0);
            p.setPf(pf);
            p.setTax(taxVal);
            p.setInsurance(insurance);
            p.setOtherDeductions(0.0);
            p.setNetSalary(net);
            p.setProcessedDate(LocalDate.now());
            p.setStatus("PAID");

            if (PAYROLL_DAO.create(p)) {
                count++;
                
                // Dispatch notification
                NotificationSystem.getInstance().notify(
                        "ALL", 
                        String.format("Dear %s, your salary of $%,.2f for the month of %s has been credited.", emp.getFirstName(), net, month),
                        emp.getId()
                );
            }
        }
        System.out.println("SUCCESS: Processed payroll for " + count + " active employees for month " + month);
    }

    @SuppressWarnings("unchecked")
    private static void exportPayrollReport(Scanner scanner) {
        System.out.print("Enter Target Month (YYYY-MM): ");
        String month = scanner.nextLine().trim();
        List<Payroll> payrolls = PAYROLL_DAO.getByMonth(month);

        if (payrolls.isEmpty()) {
            System.out.println("No payroll records processed for " + month + ".");
            return;
        }

        ReportGenerator<Payroll> generator = ReportFactory.getReportGenerator(ReportFactory.ReportType.PAYROLL);
        String report = generator.generateReport("Payroll Report - " + month, payrolls);

        // Export to CSV file simulation
        java.io.File dir = new java.io.File("c:\\Users\\Vishe\\Downloads\\javap\\exports");
        if (!dir.exists()) dir.mkdirs();

        java.io.File file = new java.io.File(dir, "payroll_report_" + month + ".csv");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(report);
            System.out.println("SUCCESS: Report successfully exported to: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("ERROR: Failed to save file.");
        }
    }

    private static void predictPayroll() {
        double prediction = ANALYTICS_SERVICE.predictNextMonthPayrollCost();
        System.out.printf("\nPredicted workforce payroll liability for next month: $%,.2f\n", prediction);
    }

    private static void runFinanceSalarySimulator(Scanner scanner) {
        System.out.print("Target Employee ID: ");
        int empId = Integer.parseInt(scanner.nextLine().trim());
        Employee emp = EMPLOYEE_DAO.getById(empId);
        if (emp == null) {
            System.out.println("Employee not found.");
            return;
        }

        System.out.print("Hypothetical Base Salary ($): ");
        double sal = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Hypothetical Allowance ($): ");
        double allow = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Tax Regime (OLD_REGIME / NEW_REGIME): ");
        String regime = scanner.nextLine().trim();

        double simulatedNet = ANALYTICS_SERVICE.simulateSalary(emp, sal, allow, regime);
        System.out.printf("SIMULATED NET MONTHLY SALARY: $%,.2f\n", simulatedNet);
    }

    // ----------------------------------------------------
    // MANAGER ACTIONS
    // ----------------------------------------------------
    private static void showManagerMenu(Scanner scanner) {
        System.out.println("1. View Reporting Hierarchy (Org Graph)");
        System.out.println("2. View Pending Leaves");
        System.out.println("3. Approve/Reject Leaves");
        System.out.println("4. AI HR Assistant Console");
        System.out.println("5. Logout");
        System.out.print("Select choice: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                viewHierarchy();
                break;
            case "2":
                viewPendingLeaves();
                break;
            case "3":
                processLeaves(scanner);
                break;
            case "4":
                runAIAssistant(scanner);
                break;
            case "5":
                handleLogout();
                break;
            default:
                System.out.println("Invalid choice.");
        }
    }

    private static void viewHierarchy() {
        if (currentEmployee == null) {
            System.out.println("No manager profile mapped to current user credentials.");
            return;
        }
        System.out.println(AI_SERVICE.processQuery("hierarchy " + currentEmployee.getId(), currentEmployee.getId()));
    }

    private static void viewPendingLeaves() {
        if (currentEmployee == null) {
            System.out.println("No associated employee profile found.");
            return;
        }
        List<LeaveRequest> list = LEAVE_REQUEST_DAO.getPendingRequestsByManager(currentEmployee.getId());
        if (list.isEmpty()) {
            System.out.println("No pending leave requests.");
            return;
        }
        System.out.printf("\n%-5s | %-10s | %-12s | %-12s | %-12s\n", "ID", "Emp ID", "Type", "Start Date", "End Date");
        System.out.println("------------------------------------------------------------------------");
        for (LeaveRequest r : list) {
            System.out.printf("%-5d | %-10d | %-12s | %-12s | %-12s\n", 
                    r.getId(), r.getEmployeeId(), r.getLeaveType(), r.getStartDate(), r.getEndDate());
        }
    }

    private static void processLeaves(Scanner scanner) {
        if (currentEmployee == null) return;
        
        System.out.print("Enter Leave Request ID: ");
        int reqId = Integer.parseInt(scanner.nextLine().trim());
        LeaveRequest req = LEAVE_REQUEST_DAO.getById(reqId);
        
        if (req == null || req.getManagerId() != currentEmployee.getId()) {
            System.out.println("Request not found or not allocated to you.");
            return;
        }

        System.out.print("Action (APPROVE / REJECT): ");
        String action = scanner.nextLine().trim();
        System.out.print("Comment: ");
        String comment = scanner.nextLine().trim();

        // Apply State pattern transitions
        com.payrollx.state.LeaveState state = new PendingState();
        try {
            if ("APPROVE".equalsIgnoreCase(action)) {
                state.approve(req, currentEmployee.getId(), comment);
            } else {
                state.reject(req, currentEmployee.getId(), comment);
            }

            if (LEAVE_REQUEST_DAO.update(req)) {
                System.out.println("SUCCESS: Leave state updated to " + req.getStatus() + "!");
                
                // Dispatch notification
                NotificationSystem.getInstance().notify(
                        "ALL",
                        String.format("Leave Request ID %d has been %s by manager: %s", req.getId(), req.getStatus(), comment),
                        req.getEmployeeId()
                );
            }
        } catch (IllegalStateException e) {
            System.out.println("STATE ERROR: " + e.getMessage());
        }
    }

    // ----------------------------------------------------
    // EMPLOYEE ACTIONS
    // ----------------------------------------------------
    private static void showEmployeeMenu(Scanner scanner) {
        System.out.println("1. Attendance Check-In");
        System.out.println("2. Attendance Check-Out");
        System.out.println("3. Submit Leave Request");
        System.out.println("4. AI HR Assistant Console");
        System.out.println("5. Run Salary Simulator");
        System.out.println("6. Logout");
        System.out.print("Select choice: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                checkIn();
                break;
            case "2":
                checkOut();
                break;
            case "3":
                requestLeave(scanner);
                break;
            case "4":
                runAIAssistant(scanner);
                break;
            case "5":
                runEmployeeSalarySimulator(scanner);
                break;
            case "6":
                handleLogout();
                break;
            default:
                System.out.println("Invalid choice.");
        }
    }

    private static void checkIn() {
        if (currentEmployee == null) return;
        Attendance att = ATTENDANCE_DAO.getByEmployeeIdAndDate(currentEmployee.getId(), LocalDate.now());
        if (att != null) {
            System.out.println("You have already checked-in today.");
            return;
        }
        
        att = new Attendance();
        att.setEmployeeId(currentEmployee.getId());
        att.setDate(LocalDate.now());
        att.setCheckIn(LocalDateTime.now());
        att.setStatus("PRESENT");
        
        if (ATTENDANCE_DAO.create(att)) {
            System.out.println("SUCCESS: Checked-in at " + att.getCheckIn());
        }
    }

    private static void checkOut() {
        if (currentEmployee == null) return;
        Attendance att = ATTENDANCE_DAO.getByEmployeeIdAndDate(currentEmployee.getId(), LocalDate.now());
        if (att == null) {
            System.out.println("Please check-in first.");
            return;
        }
        if (att.getCheckOut() != null) {
            System.out.println("You have already checked-out today.");
            return;
        }

        att.setCheckOut(LocalDateTime.now());
        // Calculate standard overtime (over 8h shift)
        long mins = java.time.Duration.between(att.getCheckIn(), att.getCheckOut()).toMinutes();
        double hours = mins / 60.0;
        if (hours > 8.0) {
            att.setOvertimeHours(hours - 8.0);
        }

        if (ATTENDANCE_DAO.update(att)) {
            System.out.printf("SUCCESS: Checked-out! Total hours worked: %.1f hours. Overtime computed: %.1f hours.\n", 
                    hours, att.getOvertimeHours());
        }
    }

    private static void requestLeave(Scanner scanner) {
        if (currentEmployee == null) return;
        System.out.print("Leave Type (ANNUAL / SICK / EMERGENCY): ");
        String type = scanner.nextLine().trim();
        System.out.print("Start Date (YYYY-MM-DD): ");
        LocalDate start = LocalDate.parse(scanner.nextLine().trim());
        System.out.print("End Date (YYYY-MM-DD): ");
        LocalDate end = LocalDate.parse(scanner.nextLine().trim());
        System.out.print("Comment/Reason: ");
        String comment = scanner.nextLine().trim();

        LeaveRequest req = new LeaveRequest();
        req.setEmployeeId(currentEmployee.getId());
        req.setLeaveType(type);
        req.setStartDate(start);
        req.setEndDate(end);
        req.setStatus("PENDING");
        req.setManagerId(currentEmployee.getManagerId() != null ? currentEmployee.getManagerId() : 1); // fallback to manager ID or 1
        req.setComment(comment);

        if (LEAVE_REQUEST_DAO.create(req)) {
            System.out.println("SUCCESS: Leave request submitted for manager approval.");
        } else {
            System.out.println("ERROR: Request submission failed.");
        }
    }

    private static void runEmployeeSalarySimulator(Scanner scanner) {
        if (currentEmployee == null) return;
        System.out.print("Hypothetical Bonus Amount ($): ");
        double bonus = Double.parseDouble(scanner.nextLine().trim());
        System.out.print("Regime Choice (OLD_REGIME / NEW_REGIME): ");
        String regime = scanner.nextLine().trim();

        double simulatedNet = ANALYTICS_SERVICE.simulateSalary(currentEmployee, currentEmployee.getSalary(), bonus, regime);
        System.out.printf("Simulated Monthly Net: $%,.2f\n", simulatedNet);
    }

    // ----------------------------------------------------
    // COMMON HELPERS
    // ----------------------------------------------------
    private static void runAIAssistant(Scanner scanner) {
        System.out.println("\n--- PayrollX AI HR ASSISTANT CHAT CONSOLE ---");
        System.out.println("(Type 'exit' to return to menu)");
        while (true) {
            System.out.print("Ask AI HR Assistant: ");
            String prompt = scanner.nextLine().trim();
            if ("exit".equalsIgnoreCase(prompt)) {
                break;
            }
            String answer = AI_SERVICE.processQuery(prompt, currentEmployee != null ? currentEmployee.getId() : 1);
            System.out.println("\nAI Assistant:\n" + answer + "\n");
        }
    }

    private static void handleLogout() {
        if (currentUser != null) {
            AuditLog log = new AuditLog();
            log.setUserId(currentUser.getId());
            log.setAction("LOGOUT");
            log.setDetails("User " + currentUser.getUsername() + " successfully logged out.");
            log.setTimestamp(LocalDateTime.now());
            AUDIT_LOG_DAO.create(log);
        }
        currentUser = null;
        currentEmployee = null;
        System.out.println("Logged out successfully.");
    }

    private static void seedInitialData() {
        // Only seed if database is empty (roles already seeded by schema.sql)
        if (USER_DAO.getByUsername("hr_manager") != null) {
            return; // Already seeded
        }

        // 1. Create a Department
        Department engineering = new Department();
        engineering.setDeptName("Engineering");
        engineering.setBudget(500000.0);
        DEPARTMENT_DAO.create(engineering);

        Department hrDept = new Department();
        hrDept.setDeptName("Human Resources");
        hrDept.setBudget(100000.0);
        DEPARTMENT_DAO.create(hrDept);

        // 2. Create User Accounts & Employees
        // Admin user is already created in schema.sql (admin/admin123)

        // Seed HR
        User hrUser = new User();
        hrUser.setUsername("hr_manager");
        hrUser.setPassword(SecurityUtils.hashPassword("hr123"));
        hrUser.setRole(RoleType.HR);
        hrUser.setStatus("ACTIVE");
        USER_DAO.create(hrUser);

        Employee hrEmp = new Employee.Builder()
                .userId(hrUser.getId())
                .firstName("Sarah")
                .lastName("Connor")
                .email("sarah.c@company.com")
                .phone("9999888877")
                .hireDate(LocalDate.now().minusYears(3))
                .departmentId(hrDept.getId())
                .position("HR Generalist")
                .salary(7500.0)
                .bankAccount("BANK_HR_101")
                .build();
        EMPLOYEE_DAO.create(hrEmp);

        // Seed Finance
        User finUser = new User();
        finUser.setUsername("finance_analyst");
        finUser.setPassword(SecurityUtils.hashPassword("fin123"));
        finUser.setRole(RoleType.FINANCE);
        finUser.setStatus("ACTIVE");
        USER_DAO.create(finUser);

        Employee finEmp = new Employee.Builder()
                .userId(finUser.getId())
                .firstName("Michael")
                .lastName("Scott")
                .email("michael.s@company.com")
                .phone("8888777766")
                .hireDate(LocalDate.now().minusYears(2))
                .departmentId(hrDept.getId())
                .position("Financial Controller")
                .salary(8200.0)
                .bankAccount("BANK_FIN_202")
                .build();
        EMPLOYEE_DAO.create(finEmp);

        // Seed Manager
        User mgrUser = new User();
        mgrUser.setUsername("john_manager");
        mgrUser.setPassword(SecurityUtils.hashPassword("mgr123"));
        mgrUser.setRole(RoleType.MANAGER);
        mgrUser.setStatus("ACTIVE");
        USER_DAO.create(mgrUser);

        Employee mgrEmp = new Employee.Builder()
                .userId(mgrUser.getId())
                .firstName("John")
                .lastName("Smith")
                .email("john.smith@company.com")
                .phone("7777666655")
                .hireDate(LocalDate.now().minusYears(4))
                .departmentId(engineering.getId())
                .position("Engineering Director")
                .salary(12000.0)
                .bankAccount("BANK_MGR_303")
                .build();
        EMPLOYEE_DAO.create(mgrEmp);
        
        // Map manager back to department
        engineering.setManagerId(mgrEmp.getId());
        DEPARTMENT_DAO.update(engineering);

        // Seed Standard Employee reporting to John Smith
        User devUser = new User();
        devUser.setUsername("alice_dev");
        devUser.setPassword(SecurityUtils.hashPassword("dev123"));
        devUser.setRole(RoleType.EMPLOYEE);
        devUser.setStatus("ACTIVE");
        USER_DAO.create(devUser);

        Employee devEmp = new Employee.Builder()
                .userId(devUser.getId())
                .firstName("Alice")
                .lastName("Jones")
                .email("alice.j@company.com")
                .phone("6666555544")
                .hireDate(LocalDate.now().minusYears(1))
                .departmentId(engineering.getId())
                .managerId(mgrEmp.getId())
                .position("Software Engineer")
                .salary(6000.0)
                .bankAccount("BANK_DEV_404")
                .build();
        EMPLOYEE_DAO.create(devEmp);

        // Seed some history logs and performance
        Performance perf = new Performance();
        perf.setEmployeeId(devEmp.getId());
        perf.setRating(4.5);
        perf.setKpiScore(90.0);
        perf.setFeedback("Excellent engineering support and coding skills.");
        perf.setEvaluationDate(LocalDate.now().minusMonths(1));
        PERFORMANCE_DAO.create(perf);

        // Rebuild trie
        AI_SERVICE.refreshDSAStructures();
    }
}
