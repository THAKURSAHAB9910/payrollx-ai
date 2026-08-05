const url = require('url');
const crypto = require('crypto');

// In-Memory Database State (Reset when lambda goes completely cold, which is perfect for demo restarts)
const dataStore = {
  users: [
    { id: 1, username: 'admin', password: 'admin123', passwordHash: '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', role: 'ADMIN', employeeId: null },
    { id: 2, username: 'hr_manager', password: 'hr123', passwordHash: '4153ea45b0a33118cf94cfdcd460d70b67484df61b0a8809ff4cfc5874c7e8e8', role: 'HR', employeeId: 1 },
    { id: 3, username: 'finance_analyst', password: 'fin123', passwordHash: 'a571f54070a2f7c00e12d5cd5e810a952c42c75a40234e40e6c4e0c0c0c0c0c0', role: 'FINANCE', employeeId: 2 },
    { id: 4, username: 'john_manager', password: 'mgr123', passwordHash: '8c9834240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c', role: 'MANAGER', employeeId: 3 },
    { id: 5, username: 'alice_dev', password: 'dev123', passwordHash: '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', role: 'EMPLOYEE', employeeId: 4 }
  ],
  employees: [
    { id: 1, userId: 2, firstName: 'Sarah', lastName: 'Connor', email: 'sarah.c@company.com', phone: '9999888877', position: 'HR Generalist', salary: 7500.0, status: 'ACTIVE', departmentId: 2, managerId: null, bankAccount: 'BANK_HR_101' },
    { id: 2, userId: 3, firstName: 'Michael', lastName: 'Scott', email: 'michael.s@company.com', phone: '8888777766', position: 'Financial Controller', salary: 8200.0, status: 'ACTIVE', departmentId: 2, managerId: null, bankAccount: 'BANK_FIN_202' },
    { id: 3, userId: 4, firstName: 'John', lastName: 'Smith', email: 'john.smith@company.com', phone: '7777666655', position: 'Engineering Director', salary: 12000.0, status: 'ACTIVE', departmentId: 1, managerId: null, bankAccount: 'BANK_MGR_303' },
    { id: 4, userId: 5, firstName: 'Alice', lastName: 'Jones', email: 'alice.j@company.com', phone: '6666555544', position: 'Software Engineer', salary: 6000.0, status: 'ACTIVE', departmentId: 1, managerId: 3, bankAccount: 'BANK_DEV_404' }
  ],
  departments: [
    { id: 1, name: 'Engineering', budget: 500000.0, managerId: 3, spent: 18000.0 },
    { id: 2, name: 'Human Resources', budget: 100000.0, managerId: null, spent: 15700.0 }
  ],
  leaves: [
    { id: 1, employeeId: 4, leaveType: 'ANNUAL', startDate: '2026-08-10', endDate: '2026-08-15', status: 'PENDING', managerId: 3, comment: 'Visiting family' },
    { id: 2, employeeId: 4, leaveType: 'SICK', startDate: '2026-07-01', endDate: '2026-07-03', status: 'APPROVED', managerId: 3, comment: 'Doctor prescription' }
  ],
  anomalies: [
    { type: 'DUPLICATE_PAYROLL', severity: 'HIGH', details: 'Identical salary amount issued twice to Sarah Connor in YYYY-MM ledger.' },
    { type: 'ATTENDANCE_CLOCK_OUT_BEFORE_IN', severity: 'MEDIUM', details: 'Employee ID 4 checked out at 09:00:00 before checking in today.' }
  ],
  logs: []
};

// Helper to hash passwords using SHA-256 (matching backend encryption)
function hashSHA256(str) {
  return crypto.createHash('sha256').update(str).digest('hex');
}

// JSON body parser helper for serverless functions
function getRequestBody(req) {
  return new Promise((resolve) => {
    let body = '';
    req.on('data', chunk => { body += chunk.toString(); });
    req.on('end', () => {
      try {
        resolve(JSON.parse(body));
      } catch (e) {
        resolve({});
      }
    });
  });
}

module.exports = async (req, res) => {
  // CORS Headers
  res.setHeader('Access-Control-Allow-Credentials', true);
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS,PATCH,DELETE,POST,PUT');
  res.setHeader(
    'Access-Control-Allow-Headers',
    'X-CSRF-Token, X-Requested-With, Accept, Accept-Version, Content-Length, Content-MD5, Content-Type, Date, X-Api-Version'
  );

  if (req.method === 'OPTIONS') {
    res.status(200).end();
    return;
  }

  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;
  const query = parsedUrl.query;

  // ROUTER CONTROLLERS
  try {
    // 1. POST /api/auth/login
    if (pathname === '/api/auth/login' && req.method === 'POST') {
      const { username, password } = await getRequestBody(req);
      const user = dataStore.users.find(u => u.username === username);
      const inputHash = hashSHA256(password);
      
      if (user && (user.password === password || user.passwordHash === inputHash)) {
        res.status(200).json({
          success: true,
          token: 'mock_token_' + Math.random().toString(36).substr(2),
          role: user.role,
          userId: user.employeeId || 1,
          name: user.username
        });
      } else {
        res.status(401).json({ success: false, message: 'Invalid credentials.' });
      }
      return;
    }

    // 2. POST /api/auth/register
    if (pathname === '/api/auth/register' && req.method === 'POST') {
      const data = await getRequestBody(req);
      const newUserId = dataStore.users.length + 1;
      const newEmpId = dataStore.employees.length + 1;

      dataStore.users.push({
        id: newUserId,
        username: data.username,
        password: data.password,
        passwordHash: hashSHA256(data.password),
        role: 'EMPLOYEE',
        employeeId: newEmpId
      });

      dataStore.employees.push({
        id: newEmpId,
        userId: newUserId,
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        phone: data.phone,
        position: 'Software Engineer',
        salary: 5000.0,
        status: 'ACTIVE',
        departmentId: 1,
        managerId: 3,
        bankAccount: data.bankAccount
      });

      res.status(200).json({ success: true });
      return;
    }

    // 3. GET /api/employee/profile
    if (pathname === '/api/employee/profile' && req.method === 'GET') {
      const empId = parseInt(query.employeeId);
      const emp = dataStore.employees.find(e => e.id === empId);
      if (emp) {
        res.status(200).json({
          success: true,
          id: emp.id,
          firstName: emp.firstName,
          lastName: emp.lastName,
          email: emp.email,
          phone: emp.phone,
          position: emp.position,
          salary: emp.salary,
          status: emp.status,
          bankAccount: emp.bankAccount
        });
      } else {
        res.status(404).json({ success: false, message: 'Profile not found.' });
      }
      return;
    }

    // 4. GET /api/dashboard/stats
    if (pathname === '/api/dashboard/stats' && req.method === 'GET') {
      const totalCost = dataStore.employees
        .filter(e => e.status === 'ACTIVE')
        .reduce((sum, e) => sum + e.salary, 0);

      // Recalculate department spent dynamically
      const updatedDepts = dataStore.departments.map(d => {
        const spent = dataStore.employees
          .filter(e => e.departmentId === d.id && e.status === 'ACTIVE')
          .reduce((sum, e) => sum + e.salary, 0);
        return {
          name: d.name,
          budget: d.budget,
          spent: spent,
          utilization: (spent / d.budget) * 100
        };
      });

      res.status(200).json({
        activeEmployees: dataStore.employees.filter(e => e.status === 'ACTIVE').length,
        monthlyCost: totalCost,
        departments: updatedDepts
      });
      return;
    }

    // 5. GET /api/employees
    if (pathname === '/api/employees' && req.method === 'GET') {
      res.status(200).json(dataStore.employees);
      return;
    }

    // 6. POST /api/employees/update-salary
    if (pathname === '/api/employees/update-salary' && req.method === 'POST') {
      const { employeeId, newSalary, reason } = await getRequestBody(req);
      const emp = dataStore.employees.find(e => e.id === parseInt(employeeId));
      if (emp) {
        emp.salary = parseFloat(newSalary);
        dataStore.logs.push({
          action: 'UPDATE_SALARY',
          details: `Updated salary of ${emp.firstName} ${emp.lastName} to $${newSalary}. Reason: ${reason}`
        });
        res.status(200).json({ success: true });
      } else {
        res.status(404).json({ success: false, message: 'Employee not found.' });
      }
      return;
    }

    // 7. GET /api/leaves/my
    if (pathname === '/api/leaves/my' && req.method === 'GET') {
      const empId = parseInt(query.employeeId);
      const list = dataStore.leaves.filter(l => l.employeeId === empId);
      res.status(200).json(list);
      return;
    }

    // 8. GET /api/leaves/pending
    if (pathname === '/api/leaves/pending' && req.method === 'GET') {
      const managerId = query.managerId ? parseInt(query.managerId) : null;
      let list = dataStore.leaves.filter(l => l.status === 'PENDING');
      if (managerId) {
        list = list.filter(l => l.managerId === managerId);
      }
      res.status(200).json(list);
      return;
    }

    // 9. POST /api/leaves/submit
    if (pathname === '/api/leaves/submit' && req.method === 'POST') {
      const data = await getRequestBody(req);
      const newLeave = {
        id: dataStore.leaves.length + 1,
        employeeId: parseInt(data.employeeId),
        leaveType: data.leaveType,
        startDate: data.startDate,
        endDate: data.endDate,
        status: 'PENDING',
        managerId: 3, // John Smith as default manager
        comment: data.reason
      };
      dataStore.leaves.push(newLeave);
      res.status(200).json({ success: true });
      return;
    }

    // 10. POST /api/leaves/action
    if (pathname === '/api/leaves/action' && req.method === 'POST') {
      const { id, action, comment } = await getRequestBody(req);
      const leave = dataStore.leaves.find(l => l.id === parseInt(id));
      if (leave) {
        leave.status = action === 'APPROVE' ? 'APPROVED' : 'REJECTED';
        leave.comment = comment;
        res.status(200).json({ success: true });
      } else {
        res.status(404).json({ success: false, message: 'Request not found.' });
      }
      return;
    }

    // 11. GET /api/anomalies
    if (pathname === '/api/anomalies' && req.method === 'GET') {
      res.status(200).json(dataStore.anomalies);
      return;
    }

    // 12. GET /api/promotions
    if (pathname === '/api/promotions' && req.method === 'GET') {
      const recommendations = [
        { employeeName: 'Alice Jones', rating: 4.5, score: 92.5 },
        { employeeName: 'Sarah Connor', rating: 4.0, score: 81.0 }
      ];
      res.status(200).json(recommendations);
      return;
    }

    // 13. POST /api/analytics/simulate
    if (pathname === '/api/analytics/simulate' && req.method === 'POST') {
      const { baseSalary, bonus, taxRegime } = await getRequestBody(req);
      const gross = baseSalary + (baseSalary * 0.40) + 3600.0; // stack allowances
      const pf = baseSalary * 0.12;
      const taxRate = taxRegime === 'NEW_REGIME' ? 0.15 : 0.20;
      const taxVal = (gross * 12 * taxRate) / 12;
      const net = gross + bonus - (pf + taxVal + 1000.0);
      
      res.status(200).json({ success: true, simulatedNet: net });
      return;
    }

    // 14. POST /api/ai/query
    if (pathname === '/api/ai/query' && req.method === 'POST') {
      const { query } = await getRequestBody(req);
      const lower = query.toLowerCase();
      let answer = '';

      if (lower.includes('help')) {
        answer = 'Available commands: "help" for instructions, "hierarchy" to see your manager tree, "salary" to get basic wage averages.';
      } else if (lower.includes('hierarchy') || lower.includes('org')) {
        answer = 'Reporting Hierarchy Chain:\nSarah Connor (ID 1, HR) -> John Smith (ID 3, Director) -> Alice Jones (ID 4, Engineer).';
      } else if (lower.includes('salary') || lower.includes('wage')) {
        answer = 'The average base salary for the workforce is $8,425.00 per month.';
      } else {
        answer = 'Hello! I am your PayrollX AI assistant. I can help compute promotion scoring, audit directories, and list hierarchy structures. Try asking about "hierarchy" or "salary"!';
      }

      res.status(200).json({ answer: answer });
      return;
    }

    // 404 Route Fallback
    res.status(404).json({ success: false, message: 'Route not found.' });

  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
};
