# PayrollX AI – Enterprise Employee Payroll & Workforce Management System

PayrollX AI is a production-quality enterprise HR and payroll management platform built using Core Java, SQL, Custom Data Structures & Algorithms, and robust Software Design Patterns. It mimics real-world enterprise architectures to achieve scalability, security, and intelligent workforce insights.

---

## Technical Stack & Architecture
- **Language**: Core Java (JDK 17 LTS)
- **Database**: H2 Database Engine (In-Memory embedded mode by default for zero-setup, configurable to MySQL or PostgreSQL)
- **Database Connectivity**: JDBC (Connection pool, transaction management, prepared statements)
- **Build Tool**: Maven (automated test and compilation)
- **Testing**: JUnit 5 Jupiter engine
- **Design Architecture**: Layered MVC (Controller -> Service -> DAO/Repository -> Model)

---

## Enterprise Design Patterns Implemented
1. **Singleton**: `DatabaseConnectionManager` controls connection resources.
2. **Factory**:
   - `EmployeeFactory` configures employee templates (`FULL_TIME`, `CONTRACTOR`, `INTERN`).
   - `ReportFactory` instantiates report generators.
3. **Builder**: `Employee.Builder` handles construction of complex employee records.
4. **Strategy**:
   - `TaxCalculationStrategy` (Old Regime vs. New Regime calculations).
   - `BonusCalculationStrategy` (Performance-based vs. Tenure-based calculations).
5. **Observer**: `NotificationSystem` dispatches alerts (Email, SMS, In-App simulations) dynamically to registered listeners on key payroll events.
6. **Command**: `UpdateSalaryCommand` and `PromoteEmployeeCommand` support administrative transactions and rollback.
7. **State**: `LeaveRequest` state machine manages transitions (`Pending` -> `Approved`/`Rejected`).
8. **Decorator**: `SalaryComponent` decorated dynamically with allowances (`HraDecorator`, `MedicalAllowanceDecorator`, `TravelAllowanceDecorator`).
9. **Template Method**: `ReportGenerator` outlines header/body/footer rendering steps for rosters and salary sheets.
10. **Repository & DAO**: Decouples business logic from JDBC SQL queries.

---

## Custom DSA Implementations
To solve core business logic constraints efficiently, PayrollX AI implements custom data structures instead of standard library utilities:
- **Trie (Employee Autocomplete)**: Provides prefix-matching search suggestions for employee names in \(O(K)\) time where \(K\) is name length.
- **OrgGraph (Hierarchy Traversals)**: Represents the reporting chain using a directed graph. Supports BFS (reporting layers) and DFS (depth-first org tree) to calculate chain of command and search subordinates.
- **LRU Cache (Employee Cache)**: Custom Doubly Linked List + HashMap implementation cache to achieve \(O(1)\) lookup of employee records and reduce DB load.
- **Segment Tree (Salary Range Query)**: Runs range queries on salaries (e.g., total salary cost or max salary in a range of employee IDs) in \(O(\log N)\) time.
- **CommandStack**: Custom stack storing administrative operations to support multi-step undo operations.
- **Sorter (Merge/Quick Sort)**: Custom sorting implementations to order employee performance lists or salary costs.

---

## Core Features & Modules
### 1. Authentication
Role-Based Access Control (RBAC) with hashed passwords (SHA-256) supporting:
- **ADMIN**: Manage employees, departments, run fraud checks, undo actions, view logs.
- **HR**: Employee rosters, computed promotions, AI Assistant.
- **FINANCE**: Process monthly payroll, view/export reports, predict payroll cost.
- **MANAGER**: View hierarchy tree, approve/reject leaves, AI Assistant.
- **EMPLOYEE**: Clock-in/Clock-out (attendance tracking), request leaves, salary simulation.

### 2. AI HR Assistant
A natural language query processor responding to:
- `"show my salary"`
- `"salary breakup"`
- `"remaining leave balance"`
- `"tax deduction explanation"`
- `"download payslip"`
- `"autocomplete <name_prefix>"`
- `"hierarchy <employee_id>"`

### 3. Payroll Anomaly Detection
Checks database integrity and flags:
- **Duplicate Attendance**: Overlapping attendance check-ins.
- **Duplicate Bank Account**: Separate profiles sharing identical banking numbers.
- **Impossible Overtime**: Daily shift length > 18 hours or overtime > 8 hours.
- **Ghost Employees**: Active credentials with zero attendance or active user login without employee profiles.

### 4. Predictive Analytics
Uses linear regression over historical data to project future workforce payroll liability.

---

## Quick Setup & Execution

### Prerequisites
- JDK 17 installed
- Windows PowerShell

### Setup Steps
1. Clone or copy the project files to your workspace directory.
2. Right-click and choose **Run with PowerShell** on `build.ps1` or run the following command in PowerShell:
   ```powershell
   powershell -ExecutionPolicy Bypass -File build.ps1
   ```
   *Note: This script automatically configures JDK 17, downloads a local portable copy of Apache Maven, resolves H2 database scripts, compiles the code, and triggers all JUnit tests.*

3. Once built, run the application using:
   ```powershell
   powershell -Command "$env:PATH='c:\Users\Vishe\Downloads\javap\maven\apache-maven-3.9.6\bin;' + $env:PATH; mvn exec:java -Dexec.mainClass='com.payrollx.Main'"
   ```
4. Use the default seeded logins to test different role profiles:
   - **Admin**: `admin` / `admin123`
   - **HR**: `hr_manager` / `hr123`
   - **Finance**: `finance_analyst` / `fin123`
   - **Manager**: `john_manager` / `mgr123`
   - **Employee**: `alice_dev` / `dev123`
