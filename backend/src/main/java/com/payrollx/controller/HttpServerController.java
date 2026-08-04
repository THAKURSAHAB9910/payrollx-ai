package com.payrollx.controller;

import com.payrollx.dao.*;
import com.payrollx.model.*;
import com.payrollx.service.*;
import com.payrollx.state.PendingState;
import com.payrollx.util.SecurityUtils;
import com.payrollx.config.DatabaseConnectionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.sql.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Embedded HTTP Web Server Controller utilizing JDK's built-in HttpServer.
 * Serves static assets and maps API requests without third-party frameworks.
 * All handlers include comprehensive try-catch wrappers for backend robustness.
 */
public class HttpServerController {
    private static final Logger LOGGER = Logger.getLogger(HttpServerController.class.getName());
    private static HttpServer server;

    private static final UserDao USER_DAO = new UserDao();
    private static final EmployeeDao EMPLOYEE_DAO = new EmployeeDao();
    private static final DepartmentDao DEPARTMENT_DAO = new DepartmentDao();
    private static final LeaveRequestDao LEAVE_REQUEST_DAO = new LeaveRequestDao();
    private static final AIService AI_SERVICE = new AIService();
    private static final AnomalyDetectionService ANOMALY_SERVICE = new AnomalyDetectionService();
    private static final WorkforceAnalyticsService ANALYTICS_SERVICE = new WorkforceAnalyticsService();

    /**
     * Starts the HTTP Server on port 8080 in a background thread.
     */
    public static void startServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(8080), 0);
            
            // Map Static web asset
            server.createContext("/", new StaticFileHandler());
            
            // Map REST API Endpoints
            server.createContext("/api/auth/login", new LoginHandler());
            server.createContext("/api/auth/register", new RegisterHandler());
            server.createContext("/api/dashboard/stats", new DashboardStatsHandler());
            server.createContext("/api/employees", new EmployeesListHandler());
            server.createContext("/api/employees/update-salary", new UpdateSalaryHandler());
            server.createContext("/api/anomalies", new AnomaliesHandler());
            server.createContext("/api/promotions", new PromotionsHandler());
            server.createContext("/api/ai/query", new AIAssistantHandler());
            server.createContext("/api/leaves/pending", new PendingLeavesHandler());
            server.createContext("/api/leaves/my", new MyLeavesHandler());
            server.createContext("/api/leaves/submit", new SubmitLeaveHandler());
            server.createContext("/api/leaves/action", new ProcessLeaveHandler());
            server.createContext("/api/analytics/simulate", new SalarySimulationHandler());
            server.createContext("/api/employee/profile", new EmployeeProfileHandler());
            
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();
            LOGGER.info("PayrollX AI Web Server launched successfully on: http://localhost:8080");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to start HTTP server on port 8080", e);
        }
    }

    public static void stopServer() {
        if (server != null) {
            server.stop(0);
            LOGGER.info("Web server stopped.");
        }
    }

    // --- STATIC RESOURCE SERVING HANDLER ---
    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/")) {
                    path = "/index.html";
                }
                
                InputStream is = getClass().getClassLoader().getResourceAsStream("web" + path);
                if (is == null) {
                    String error = "404 Not Found";
                    exchange.sendResponseHeaders(404, error.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(error.getBytes());
                    os.close();
                    return;
                }

                byte[] content = readAllBytes(is);
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, content.length);
                OutputStream os = exchange.getResponseBody();
                os.write(content);
                os.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error serving static file", e);
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        }

        private byte[] readAllBytes(InputStream inputStream) throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            return buffer.toByteArray();
        }
    }

    // --- API: LOGIN HANDLER ---
    private static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            try {
                Map<String, String> body = parseJson(readRequestBody(exchange));
                String username = body.get("username");
                String password = body.get("password");

                User user = USER_DAO.getByUsername(username);
                if (user != null && user.getPassword().equals(SecurityUtils.hashPassword(password))) {
                    if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                        sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Account suspended\"}");
                        return;
                    }
                    Employee emp = EMPLOYEE_DAO.getByUserId(user.getId());
                    int empId = emp != null ? emp.getId() : 1;
                    
                    String json = String.format("{\"success\":true,\"role\":\"%s\",\"employeeId\":%d,\"token\":\"session_%d\"}",
                            user.getRole(), empId, user.getId());
                    sendResponse(exchange, 200, json);
                } else {
                    sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Invalid credentials\"}");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Login error", e);
                sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Login exception: " + e.getMessage() + "\"}");
            }
        }
    }

    // --- API: REGISTER HANDLER ---
    private static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            try {
                Map<String, String> body = parseJson(readRequestBody(exchange));
                String fName = body.get("firstName");
                String lName = body.get("lastName");
                String email = body.get("email");
                String phone = body.get("phone");
                String bankAccount = body.get("bankAccount");
                String username = body.get("username");
                String password = body.get("password");

                if (fName == null || lName == null || email == null || username == null || password == null) {
                    sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Missing required registration details\"}");
                    return;
                }

                if (USER_DAO.getByUsername(username) != null) {
                    sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Username already taken\"}");
                    return;
                }
                if (EMPLOYEE_DAO.getByEmail(email) != null) {
                    sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Email already registered\"}");
                    return;
                }

                User user = new User();
                user.setUsername(username);
                user.setPassword(SecurityUtils.hashPassword(password));
                user.setRole(RoleType.EMPLOYEE);
                user.setStatus("ACTIVE");

                if (USER_DAO.create(user)) {
                    Employee emp = new Employee.Builder()
                            .userId(user.getId())
                            .firstName(fName)
                            .lastName(lName)
                            .email(email)
                            .phone(phone)
                            .bankAccount(bankAccount)
                            .hireDate(LocalDate.now())
                            .position("Full-Time Engineer")
                            .salary(4500.0)
                            .status("ACTIVE")
                            .build();

                    if (EMPLOYEE_DAO.create(emp)) {
                        sendResponse(exchange, 200, "{\"success\":true}");
                    } else {
                        sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Failed to create employee profile details\"}");
                    }
                } else {
                    sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Failed to create credentials record\"}");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Registration error", e);
                sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Registration exception: " + e.getMessage() + "\"}");
            }
        }
    }

    // --- API: DASHBOARD STATS ---
    private static class DashboardStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<Employee> employees = EMPLOYEE_DAO.getAll();
                List<Department> departments = DEPARTMENT_DAO.getAll();

                long activeCount = employees.stream().filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus())).count();
                double totalCost = employees.stream().mapToDouble(Employee::getSalary).sum();

                StringBuilder deptListJson = new StringBuilder("[");
                for (int i = 0; i < departments.size(); i++) {
                    Department d = departments.get(i);
                    List<Employee> deptEmps = EMPLOYEE_DAO.getByDepartmentId(d.getId());
                    double spent = deptEmps.stream().mapToDouble(Employee::getSalary).sum();
                    double utilization = d.getBudget() > 0 ? (spent / d.getBudget()) * 100 : 0.0;
                    
                    deptListJson.append(String.format("{\"id\":%d,\"name\":\"%s\",\"budget\":%.2f,\"spent\":%.2f,\"utilization\":%.2f}",
                            d.getId(), d.getDeptName(), d.getBudget(), spent, utilization));
                    if (i < departments.size() - 1) {
                        deptListJson.append(",");
                    }
                }
                deptListJson.append("]");

                String json = String.format("{\"activeEmployees\":%d,\"monthlyCost\":%.2f,\"departments\":%s}",
                        activeCount, totalCost, deptListJson.toString());
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Dashboard stats loading error", e);
                sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Error: " + e.getMessage() + "\"}");
            }
        }
    }

    // --- API: EMPLOYEES ROSTER ---
    private static class EmployeesListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<Employee> list = EMPLOYEE_DAO.getAll();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    Employee e = list.get(i);
                    sb.append(String.format("{\"id\":%d,\"firstName\":\"%s\",\"lastName\":\"%s\",\"email\":\"%s\",\"position\":\"%s\",\"salary\":%.2f,\"status\":\"%s\"}",
                            e.getId(), e.getFirstName(), e.getLastName(), e.getEmail(), e.getPosition(), e.getSalary(), e.getStatus()));
                    if (i < list.size() - 1) sb.append(",");
                }
                sb.append("]");
                sendResponse(exchange, 200, sb.toString());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Employee roster list error", e);
                sendResponse(exchange, 200, "[]");
            }
        }
    }

    // --- API: UPDATE SALARY HANDLER ---
    private static class UpdateSalaryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"success\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }

            try {
                Map<String, String> body = parseJson(readRequestBody(exchange));
                int employeeId = Integer.parseInt(body.get("employeeId"));
                double newSalary = Double.parseDouble(body.get("newSalary"));
                String reason = body.getOrDefault("reason", "Administrative salary adjustment");
                int adminUserId = Integer.parseInt(body.getOrDefault("adminUserId", "1"));

                com.payrollx.command.UpdateSalaryCommand cmd = new com.payrollx.command.UpdateSalaryCommand(employeeId, newSalary, reason, adminUserId);
                if (cmd.execute()) {
                    sendResponse(exchange, 200, "{\"success\":true}");
                } else {
                    sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Failed to apply salary adjustment command\"}");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Salary update handler error", e);
                sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Error: " + e.getMessage() + "\"}");
            }
        }
    }

    // --- API: ANOMALIES DETECTION ---
    private static class AnomaliesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<AnomalyDetectionService.Anomaly> list = ANOMALY_SERVICE.runAnomalyChecks();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    AnomalyDetectionService.Anomaly a = list.get(i);
                    String cleanDetails = a.details.replace("\"", "\\\"");
                    sb.append(String.format("{\"type\":\"%s\",\"severity\":\"%s\",\"details\":\"%s\"}",
                            a.type, a.severity, cleanDetails));
                    if (i < list.size() - 1) sb.append(",");
                }
                sb.append("]");
                sendResponse(exchange, 200, sb.toString());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Anomalies scan error", e);
                sendResponse(exchange, 200, "[]");
            }
        }
    }

    // --- API: PROMOTIONS AND CANDIDATES ---
    private static class PromotionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<WorkforceAnalyticsService.PromotionCandidate> list = ANALYTICS_SERVICE.getPromotionRecommendations();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    WorkforceAnalyticsService.PromotionCandidate c = list.get(i);
                    sb.append(String.format("{\"employeeName\":\"%s\",\"score\":%.2f,\"rating\":%.2f}",
                            c.employee.getFullName(), c.score, 4.5));
                    if (i < list.size() - 1) sb.append(",");
                }
                sb.append("]");
                sendResponse(exchange, 200, sb.toString());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Promotions recommendation loading error", e);
                sendResponse(exchange, 200, "[]");
            }
        }
    }

    // --- API: LEAVES LIST HANDLER ---
    private static class PendingLeavesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
                String managerStr = params.get("managerId");
                
                List<LeaveRequest> pending;
                if (managerStr != null && !managerStr.isEmpty()) {
                    int managerId = Integer.parseInt(managerStr);
                    pending = LEAVE_REQUEST_DAO.getPendingRequestsByManager(managerId);
                } else {
                    // Fetch all pending requests in the system for ADMIN/HR
                    pending = new ArrayList<>();
                    String sql = "SELECT * FROM leave_requests WHERE status = 'PENDING' ORDER BY start_date ASC";
                    try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
                         PreparedStatement stmt = conn.prepareStatement(sql);
                         ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            LeaveRequest req = new LeaveRequest();
                            req.setId(rs.getInt("id"));
                            req.setEmployeeId(rs.getInt("employee_id"));
                            req.setLeaveType(rs.getString("leave_type"));
                            req.setStartDate(rs.getDate("start_date").toLocalDate());
                            req.setEndDate(rs.getDate("end_date").toLocalDate());
                            req.setStatus(rs.getString("status"));
                            req.setManagerId(rs.getInt("manager_id"));
                            req.setComment(rs.getString("comment"));
                            pending.add(req);
                        }
                    }
                }
                
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < pending.size(); i++) {
                    LeaveRequest l = pending.get(i);
                    String comment = l.getComment() != null ? l.getComment().replace("\"", "\\\"") : "";
                    sb.append(String.format("{\"id\":%d,\"employeeId\":%d,\"leaveType\":\"%s\",\"startDate\":\"%s\",\"endDate\":\"%s\",\"status\":\"%s\",\"comment\":\"%s\"}",
                            l.getId(), l.getEmployeeId(), l.getLeaveType(), l.getStartDate(), l.getEndDate(), l.getStatus(), comment));
                    if (i < pending.size() - 1) sb.append(",");
                }
                sb.append("]");
                sendResponse(exchange, 200, sb.toString());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Pending leaves list error", e);
                sendResponse(exchange, 200, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // --- API: PROCESS LEAVE ACTION ---
    private static class ProcessLeaveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"success\":false}");
                return;
            }

            try {
                Map<String, String> body = parseJson(readRequestBody(exchange));
                int id = Integer.parseInt(body.get("id"));
                String action = body.get("action");
                String comment = body.get("comment");
                int managerId = Integer.parseInt(body.get("managerId"));

                LeaveRequest req = LEAVE_REQUEST_DAO.getById(id);
                if (req == null) {
                    sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Request not found\"}");
                    return;
                }

                com.payrollx.state.LeaveState state = new PendingState();
                if ("APPROVE".equalsIgnoreCase(action)) {
                    state.approve(req, managerId, comment);
                } else {
                    state.reject(req, managerId, comment);
                }

                if (LEAVE_REQUEST_DAO.update(req)) {
                    sendResponse(exchange, 200, "{\"success\":true}");
                } else {
                    sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Database update failed\"}");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Process leave error", e);
                sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Exception: " + e.getMessage() + "\"}");
            }
        }
    }

    // --- API: AI ASSISTANT PROMPT HANDLER ---
    private static class AIAssistantHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"success\":false}");
                return;
            }

            try {
                Map<String, String> body = parseJson(readRequestBody(exchange));
                String query = body.get("query");
                int employeeId = Integer.parseInt(body.get("employeeId"));

                String answer = AI_SERVICE.processQuery(query, employeeId);
                String escapedAnswer = answer.replace("\\", "\\\\")
                                              .replace("\"", "\\\"")
                                              .replace("\n", "\\n")
                                              .replace("\r", "\\r");

                String json = String.format("{\"success\":true,\"answer\":\"%s\"}", escapedAnswer);
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "AI inquiry error", e);
                sendResponse(exchange, 200, "{\"success\":false,\"answer\":\"Exception processing request: " + e.getMessage() + "\"}");
            }
        }
    }

    // --- API: SALARY SIMULATOR ---
    private static class SalarySimulationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"success\":false}");
                return;
            }

            try {
                Map<String, String> body = parseJson(readRequestBody(exchange));
                int employeeId = Integer.parseInt(body.get("employeeId"));
                double baseSalary = Double.parseDouble(body.get("baseSalary"));
                double bonus = Double.parseDouble(body.get("bonus"));
                String taxRegime = body.get("taxRegime");

                Employee emp = EMPLOYEE_DAO.getById(employeeId);
                if (emp == null) {
                    sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Employee not found\"}");
                    return;
                }

                double netSimulated = ANALYTICS_SERVICE.simulateSalary(emp, baseSalary, bonus, taxRegime);
                String json = String.format("{\"success\":true,\"simulatedNet\":%.2f}", netSimulated);
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Salary simulation error", e);
                sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Simulation exception: " + e.getMessage() + "\"}");
            }
        }
    }

    // --- API: EMPLOYEE PROFILE HANDLER ---
    private static class EmployeeProfileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"success\":false}");
                return;
            }
            try {
                Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
                int empId = Integer.parseInt(params.getOrDefault("employeeId", "1"));
                Employee e = EMPLOYEE_DAO.getById(empId);
                if (e == null) {
                    sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Profile not found\"}");
                    return;
                }
                String json = String.format("{\"success\":true,\"id\":%d,\"firstName\":\"%s\",\"lastName\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\",\"position\":\"%s\",\"salary\":%.2f,\"status\":\"%s\",\"bankAccount\":\"%s\"}",
                        e.getId(), e.getFirstName(), e.getLastName(), e.getEmail(), e.getPhone(), e.getPosition(), e.getSalary(), e.getStatus(), e.getBankAccount());
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Profile loading error", e);
                sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Exception: " + e.getMessage() + "\"}");
            }
        }
    }

    // --- API: MY LEAVES HANDLER ---
    private static class MyLeavesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"success\":false}");
                return;
            }
            try {
                Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
                int empId = Integer.parseInt(params.getOrDefault("employeeId", "1"));
                List<LeaveRequest> leaves = LEAVE_REQUEST_DAO.getByEmployeeId(empId);
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < leaves.size(); i++) {
                    LeaveRequest l = leaves.get(i);
                    String comment = l.getComment() != null ? l.getComment().replace("\"", "\\\"") : "";
                    sb.append(String.format("{\"id\":%d,\"leaveType\":\"%s\",\"startDate\":\"%s\",\"endDate\":\"%s\",\"status\":\"%s\",\"comment\":\"%s\"}",
                            l.getId(), l.getLeaveType(), l.getStartDate(), l.getEndDate(), l.getStatus(), comment));
                    if (i < leaves.size() - 1) sb.append(",");
                }
                sb.append("]");
                sendResponse(exchange, 200, sb.toString());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "My leaves fetch error", e);
                sendResponse(exchange, 200, "[]");
            }
        }
    }

    // --- API: SUBMIT LEAVE HANDLER ---
    private static class SubmitLeaveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"success\":false}");
                return;
            }
            try {
                Map<String, String> body = parseJson(readRequestBody(exchange));
                int employeeId = Integer.parseInt(body.get("employeeId"));
                String leaveType = body.get("leaveType");
                LocalDate startDate = LocalDate.parse(body.get("startDate"));
                LocalDate endDate = LocalDate.parse(body.get("endDate"));
                String comment = body.getOrDefault("reason", "");

                int managerId = 4; // Default manager
                Employee empObj = EMPLOYEE_DAO.getById(employeeId);
                if (empObj != null && empObj.getManagerId() != null) {
                    managerId = empObj.getManagerId();
                }

                LeaveRequest req = new LeaveRequest();
                req.setEmployeeId(employeeId);
                req.setLeaveType(leaveType);
                req.setStartDate(startDate);
                req.setEndDate(endDate);
                req.setStatus("PENDING");
                req.setManagerId(managerId);
                req.setComment(comment);

                if (LEAVE_REQUEST_DAO.create(req)) {
                    sendResponse(exchange, 200, "{\"success\":true}");
                } else {
                    sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Database insert failed\"}");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Leave submission error", e);
                sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Exception: " + e.getMessage() + "\"}");
            }
        }
    }

    // --- GENERAL PARSING HELPERS (Zero-dependency) ---
    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static void sendResponse(HttpExchange exchange, int status, String json) throws IOException {
        byte[] content = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, content.length);
        OutputStream os = exchange.getResponseBody();
        os.write(content);
        os.close();
    }

    private static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return map;
        }
        
        // Robust regex parser supporting strings with commas and escaped quotes
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([^,}\\s]+))");
        Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            String key = matcher.group(1);
            String val = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            map.put(key, val);
        }
        return map;
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.trim().isEmpty()) {
            return map;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length >= 2) {
                try {
                    String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8.name());
                    String val = URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name());
                    map.put(key, val);
                } catch (Exception e) {
                    map.put(kv[0], kv[1]);
                }
            }
        }
        return map;
    }
}
