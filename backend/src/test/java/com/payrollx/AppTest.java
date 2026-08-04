package com.payrollx;

import com.payrollx.command.*;
import com.payrollx.decorator.*;
import com.payrollx.model.*;
import com.payrollx.state.*;
import com.payrollx.strategy.*;
import com.payrollx.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {

    // --- TRIE TESTS ---
    @Test
    public void testTrieSuggestions() {
        Trie trie = new Trie();
        trie.insert("Alice Jones", 1);
        trie.insert("Albert Jones", 2);
        trie.insert("Bob Smith", 3);

        Map<String, List<Integer>> suggestions = trie.getSuggestions("al");
        assertEquals(2, suggestions.size());
        assertTrue(suggestions.containsKey("alice jones"));
        assertTrue(suggestions.containsKey("albert jones"));
        assertEquals(1, suggestions.get("alice jones").get(0));

        Map<String, List<Integer>> bobMatch = trie.getSuggestions("bob");
        assertEquals(1, bobMatch.size());
        assertTrue(bobMatch.containsKey("bob smith"));
    }

    // --- ORGGRAPH TESTS ---
    @Test
    public void testOrgGraphHierarchy() {
        OrgGraph graph = new OrgGraph();
        graph.addRelationship(2, 1); // 2 reports to 1
        graph.addRelationship(3, 1); // 3 reports to 1
        graph.addRelationship(4, 2); // 4 reports to 2

        Set<Integer> directReports = graph.getDirectReports(1);
        assertEquals(2, directReports.size());
        assertTrue(directReports.contains(2));
        assertTrue(directReports.contains(3));

        List<Integer> bsfSubordinates = graph.getSubordinatesBFS(1);
        assertEquals(3, bsfSubordinates.size());
        assertEquals(2, bsfSubordinates.get(0)); // Level 1 direct report
        assertEquals(3, bsfSubordinates.get(1)); // Level 1 direct report
        assertEquals(4, bsfSubordinates.get(2)); // Level 2 indirect report (child of 2)

        List<Integer> reportingChain = graph.getReportingChain(4);
        assertEquals(3, reportingChain.size());
        assertEquals(4, reportingChain.get(0));
        assertEquals(2, reportingChain.get(1));
        assertEquals(1, reportingChain.get(2));

        assertTrue(graph.wouldIntroduceCycle(1, 4));
        assertFalse(graph.wouldIntroduceCycle(3, 4));
    }

    // --- LRU CACHE TESTS ---
    @Test
    public void testLruCache() {
        LruCache<Integer, String> cache = new LruCache<>(2);
        cache.put(1, "One");
        cache.put(2, "Two");
        assertEquals("One", cache.get(1));

        cache.put(3, "Three"); // Evicts 2 (since 1 was accessed recently)
        assertNull(cache.get(2));
        assertEquals("Three", cache.get(3));
        assertEquals("One", cache.get(1));
    }

    // --- SEGMENT TREE TESTS ---
    @Test
    public void testSegmentTreeQueries() {
        double[] salaries = {1000, 2000, 3000, 4000};
        SegmentTree tree = new SegmentTree(salaries);

        assertEquals(10000.0, tree.querySum(0, 3));
        assertEquals(5000.0, tree.querySum(1, 2));
        assertEquals(4000.0, tree.queryMax(0, 3));

        tree.update(2, 5000); // 3000 becomes 5000
        assertEquals(12000.0, tree.querySum(0, 3));
        assertEquals(5000.0, tree.queryMax(0, 3));
    }

    // --- CUSTOM SORTER TESTS ---
    @Test
    public void testCustomSorter() {
        List<Integer> list1 = new ArrayList<>(Arrays.asList(4, 2, 5, 1, 3));
        Sorter.quickSort(list1, Integer::compare);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list1);

        List<Integer> list2 = new ArrayList<>(Arrays.asList(4, 2, 5, 1, 3));
        Sorter.mergeSort(list2, Integer::compare);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list2);
    }

    // --- STRATEGY TAX TESTS ---
    @Test
    public void testTaxStrategies() {
        TaxCalculationStrategy oldRegime = new OldRegimeTaxStrategy();
        TaxCalculationStrategy newRegime = new NewRegimeTaxStrategy();

        // 600,000 annual income, 100,000 section 80C deductions
        double taxOld = oldRegime.calculateTax(600000.0, 100000.0);
        // Taxable income = 500,000. 0-250k: 0%, 250k-500k: 5% of 250k = 12500. Total with 4% cess = 13000
        assertEquals(13000.0, taxOld, 0.01);

        double taxNew = newRegime.calculateTax(600000.0, 100000.0);
        // Taxable income = 550,000 (after 50k standard deduction). 0-300k: 0%, 300k-600k: 5% of 250k = 12500. Total with cess = 13000
        assertEquals(13000.0, taxNew, 0.01);
    }

    // --- DECORATOR SALARY TESTS ---
    @Test
    public void testSalaryDecorators() {
        SalaryComponent base = new BaseSalary(5000.0);
        SalaryComponent decorated = new HraDecorator(base, 5000.0); // +40% = 2000
        decorated = new MedicalAllowanceDecorator(decorated, 2000.0); // +2000
        decorated = new TravelAllowanceDecorator(decorated, 1600.0); // +1600

        assertEquals(10600.0, decorated.getAmount());
        assertTrue(decorated.getDescription().contains("HRA (40%)"));
        assertTrue(decorated.getDescription().contains("Medical Allowance"));
        assertTrue(decorated.getDescription().contains("Travel Allowance"));
    }

    // --- STATE WORKFLOW TESTS ---
    @Test
    public void testLeaveStateTransitions() {
        LeaveRequest request = new LeaveRequest();
        request.setEmployeeId(1);
        request.setLeaveType("ANNUAL");
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(2));
        request.setStatus("PENDING");

        LeaveState pending = new PendingState();
        pending.approve(request, 9, "Have a great break!");
        assertEquals("APPROVED", request.getStatus());
        assertEquals(9, request.getManagerId());
        assertEquals("Have a great break!", request.getComment());

        LeaveState approved = new ApprovedState();
        assertThrows(IllegalStateException.class, () -> approved.approve(request, 9, "re-approve"));
    }

    // --- DATABASE AND SEEDING TESTS ---
    @Test
    public void testDatabaseLogin() {
        // Initialize Connection Manager
        com.payrollx.config.DatabaseConnectionManager.getInstance();
        
        // Check if admin user is present
        com.payrollx.dao.UserDao userDao = new com.payrollx.dao.UserDao();
        com.payrollx.model.User admin = userDao.getByUsername("admin");
        assertNotNull(admin, "Admin user should be seeded by schema.sql");
        assertEquals("ACTIVE", admin.getStatus());
        assertEquals(com.payrollx.model.RoleType.ADMIN, admin.getRole());
        
        // Check password matching
        String computedHash = com.payrollx.util.SecurityUtils.hashPassword("admin123");
        assertEquals(admin.getPassword(), computedHash, "Seeded password hash should match computed hash of 'admin123'");
    }

    @Test
    public void testEmployeeRegistration() {
        com.payrollx.config.DatabaseConnectionManager.getInstance();
        com.payrollx.dao.UserDao userDao = new com.payrollx.dao.UserDao();
        com.payrollx.dao.EmployeeDao employeeDao = new com.payrollx.dao.EmployeeDao();

        // Create a unique user
        String uniqueUsername = "test_user_" + System.currentTimeMillis();
        com.payrollx.model.User user = new com.payrollx.model.User();
        user.setUsername(uniqueUsername);
        user.setPassword("password");
        user.setRole(com.payrollx.model.RoleType.EMPLOYEE);
        user.setStatus("ACTIVE");

        assertTrue(userDao.create(user), "User creation should succeed");
        assertTrue(user.getId() > 0, "User ID should be generated");

        // Create matching Employee profile
        com.payrollx.model.Employee emp = new com.payrollx.model.Employee.Builder()
                .userId(user.getId())
                .firstName("Test")
                .lastName("Subject")
                .email("test.subject_" + System.currentTimeMillis() + "@company.com")
                .phone("123456")
                .bankAccount("BANK_TEST_999")
                .hireDate(LocalDate.now())
                .position("Full-Time Engineer")
                .salary(4500.0)
                .status("ACTIVE")
                .build();

        assertTrue(employeeDao.create(emp), "Employee profile creation should succeed");
    }
}
