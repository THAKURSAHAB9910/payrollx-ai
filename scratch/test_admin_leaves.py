import urllib.request
import json

def test_admin_leaves():
    print("--- 1. Submitting Leave Request for Employee 4 ---")
    submit_data = json.dumps({
        "employeeId": 4,
        "leaveType": "ANNUAL",
        "startDate": "2026-08-15",
        "endDate": "2026-08-20",
        "reason": "Family vacation, planning ahead."
    }).encode('utf-8')
    req = urllib.request.Request("http://localhost:8080/api/leaves/submit", data=submit_data, headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req) as res:
            print("Submission Response:", res.read().decode('utf-8'))
    except Exception as e:
        print("Submission failed:", e)
        return

    print("\n--- 2. Fetching Pending Leaves (Admin view) ---")
    try:
        with urllib.request.urlopen("http://localhost:8080/api/leaves/pending") as res:
            print("Pending Leaves (Admin):", res.read().decode('utf-8'))
    except Exception as e:
        print("Admin fetch failed:", e)

    print("\n--- 3. Fetching Pending Leaves (Manager 3 view) ---")
    try:
        with urllib.request.urlopen("http://localhost:8080/api/leaves/pending?managerId=3") as res:
            print("Pending Leaves (Manager 3):", res.read().decode('utf-8'))
    except Exception as e:
        print("Manager fetch failed:", e)

if __name__ == "__main__":
    test_admin_leaves()
