package com.payrollx.controller;

import com.payrollx.dao.*;
import com.payrollx.model.*;
import com.payrollx.service.*;
import com.payrollx.state.PendingState;
import com.payrollx.util.SecurityUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Embedded HTTP Web Server Controller utilizing JDK's built-in HttpServer.
 * Serves static assets and maps API requests without third-party frameworks.
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
            server.createContext("/api/dashboard/stats", new DashboardStatsHandler());
            server.createContext("/api/employees", new EmployeesListHandler());
            server.createContext("/api/anomalies", new AnomaliesHandler());
            server.createContext("/api/promotions", new PromotionsHandler());
            server.createContext("/api/ai/query", new AIAssistantHandler());
            server.createContext("/api/leaves/pending", new PendingLeavesHandler());
            server.createContext("/api/leaves/action", new ProcessLeaveHandler());
            server.createContext("/api/analytics/simulate", new SalarySimulationHandler());
            server.createContext("/api/auth/register", new RegisterHandler());
            server.createContext("/api/employee/profile", new EmployeeProfileHandler());
            server.createContext("/api/leaves/my", new MyLeavesHandler());
            server.createContext("/api/leaves/submit", new SubmitLeaveHandler());
            
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
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            
            // Read resource file from classpath (web/index.html)
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

            Map<String, String> body = parseJson(readRequestBody(exchange));
            String fName = body.get("firstName");
            String lName = body.get("lastName");
            String email = body.get("email");
            String phone = body.get("phone");
            String bankAccount = body.get("bankAccount");
            String username = body.get("username");
            String password = body.get("password");

            // Validations
            if (USER_DAO.getByUsername(username) != null) {
                sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Username already taken\"}");
                return;
            }
            if (EMPLOYEE_DAO.getByEmail(email) != null) {
                sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Email already registered\"}");
                return;
            }

            // Create User account
            User user = new User();
            user.setUsername(username);
            user.setPassword(SecurityUtils.hashPassword(password));
            user.setRole(RoleType.EMPLOYEE);
            user.setStatus("ACTIVE");

            if (USER_DAO.create(user)) {
                // Create Employee Profile
                Employee emp = new Employee.Builder()
                        .userId(user.getId())
                        .firstName(fName)
                        .lastName(lName)
                        .email(email)
                        .phone(phone)
                        .bankAccount(bankAccount)
                        .hireDate(LocalDate.now())
                        .position("Full-Time Engineer")
                        .salary(4500.0) // default starting salary
                        .status("ACTIVE")
                        .build();

                if (EMPLOYEE_DAO.create(emp)) {
                    sendResponse(exchange, 200, "{\"success\":true}");
                } else {
                    sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Failed to create employee details\"}");
                }
            } else {
                sendResponse(exchange, 200, "{\"success\":false,\"message\":\"Failed to create credentials\"}");
            }
        }
    }

    // --- API: DASHBOARD STATS ---
    private static class DashboardStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Employee> employees = EMPLOYEE_DAO.getAll();
            List<Department> departments = DEPARTMENT_DAO.getAll();

            long activeCount = employees.stream().filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus())).count();
            double totalCost = employees.stream().mapToDouble(Employee::getSalary).sum();

            // Build departments json list
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
        }
    }

    // --- API: EMPLOYEES ROSTER ---
    private static class EmployeesListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
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
        }
    }

    // --- API: ANOMALIES DETECTION ---
    private static class AnomaliesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<AnomalyDetectionService.Anomaly> list = ANOMALY_SERVICE.runAnomalyChecks();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                AnomalyDetectionService.Anomaly a = list.get(i);
                // Safe replacement of single/double quotes in detail string
                String cleanDetails = a.details.replace("\"", "\\\"");
                sb.append(String.format("{\"type\":\"%s\",\"severity\":\"%s\",\"details\":\"%s\"}",
                        a.type, a.severity, cleanDetails));
                if (i < list.size() - 1) sb.append(",");
            }
            sb.append("]");
            sendResponse(exchange, 200, sb.toString());
        }
    }

    // --- API: PROMOTIONS AND CANDIDATES ---
    private static class PromotionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<WorkforceAnalyticsService.PromotionCandidate> list = ANALYTICS_SERVICE.getPromotionRecommendations();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                WorkforceAnalyticsService.PromotionCandidate c = list.get(i);
                sb.append(String.format("{\"employeeName\":\"%s\",\"score\":%.2f,\"rating\":%.2f}",
                        c.employee.getFullName(), c.score, 4.5)); // rating default mocks
                if (i < list.size() - 1) sb.append(",");
            }
            sb.append("]");
            sendResponse(exchange, 200, sb.toString());
        }
    }

    // --- API: LEAVES LIST HANDLER ---
    private static class PendingLeavesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<LeaveRequest> list = LEAVE_REQUEST_DAO.getByEmployeeId(4); // Seeded manager john_manager employee ID has reports. Let's list all pending.
            // Or grab all where status = 'PENDING'
            List<LeaveRequest> all = LEAVE_REQUEST_DAO.getByManagerId(4);
            List<LeaveRequest> pending = all.stream().filter(l -> "PENDING".equalsIgnoreCase(l.getStatus())).collect(Collectors.toList());
            
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < pending.size(); i++) {
                LeaveRequest l = pending.get(i);
                sb.append(String.format("{\"id\":%d,\"employeeId\":%d,\"leaveType\":\"%s\",\"startDate\":\"%s\",\"endDate\":\"%s\",\"status\":\"%s\"}",
                        l.getId(), l.getEmployeeId(), l.getLeaveType(), l.getStartDate(), l.getEndDate(), l.getStatus()));
                if (i < pending.size() - 1) sb.append(",");
            }
            sb.append("]");
            sendResponse(exchange, 200, sb.toString());
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
            try {
                if ("APPROVE".equalsIgnoreCase(action)) {
                    state.approve(req, managerId, comment);
                } else {
                    state.reject(req, managerId, comment);
                }

                if (LEAVE_REQUEST_DAO.update(req)) {
                    sendResponse(exchange, 200, "{\"success\":true}");
                } else {
                    sendResponse(exchange, 200, "{\"success\":false,\"message\":\"DB Write failed\"}");
                }
            } catch (IllegalStateException e) {
                sendResponse(exchange, 200, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
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

            Map<String, String> body = parseJson(readRequestBody(exchange));
            String query = body.get("query");
            int employeeId = Integer.parseInt(body.get("employeeId"));

            String answer = AI_SERVICE.processQuery(query, employeeId);
            // Escape json quotes and newlines
            String escapedAnswer = answer.replace("\\", "\\\\")
                                          .replace("\"", "\\\"")
                                          .replace("\n", "\\n")
                                          .replace("\r", "\\r");

            String json = String.format("{\"success\":true,\"answer\":\"%s\"}", escapedAnswer);
            sendResponse(exchange, 200, json);
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
        }
    }

    // --- GENERAL JSON PARSING HELPERS (Zero-dependency) ---
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

        // Strip curly braces
        String clean = json.trim().substring(1, json.trim().length() - 1);
        String[] tokens = clean.split(",");
        for (String token : tokens) {
            String[] kv = token.split(":");
            if (kv.length >= 2) {
                String key = kv[0].trim().replace("\"", "");
                // Handle raw values that might have colons (like URIs, times, etc.)
                String val = Arrays.stream(kv).skip(1).collect(Collectors.joining(":")).trim().replace("\"", "");
                map.put(key, val);
            }
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
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }
}
