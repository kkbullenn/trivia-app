
# 🎓 Trivia App — Class Database Integration Guide

**Course:** COMP 3940 — Client-Server Applications  
**Project:** Trivia App  
**Database Host:** Railway MySQL  
**DB Team** Haider/ Haven/ Jessie  

---

## 🧭 Overview
This document explains how **all teams** (Database, Backend, Frontend, and QA) connect to and work with the shared **Trivia App MySQL database** hosted on **Railway**.

No passwords are stored in this document — each team receives their credentials privately.

---

## 🧱 1. Database Information

| Item | Value |
|------|-------|
| **Database name** | `trivia_app` |
| **Host** | `shuttle.proxy.rlwy.net` |
| **Port** | `24339` |
| **Engine / Version** | MySQL 8.0 (utf8mb4) |
| **SSL** | Required |
| **Timezone** | UTC |
| **Environment** | Railway Cloud |

---

## 🧩 2. Connection Instructions (All Teams)

### Using MySQL Workbench
1. Open **MySQL Workbench** → click the **“+”** next to *MySQL Connections*.
2. Fill out the following fields:
   - **Connection Name:** `TriviaApp (Your Team)`
   - **Hostname:** `shuttle.proxy.rlwy.net`
   - **Port:** `24339`
   - **Username:** (your assigned team username)
   - **Password:** (use “Store in Vault…” to save your private password)
   - **Default Schema:** `trivia_app`
3. Click the **SSL tab → Use SSL = Required**.
4. Click **Test Connection → OK**.

### Using the Command Line
```bash
mysql -h shuttle.proxy.rlwy.net -P 24339 -u <your_team_username> -p trivia_app
# Enter your team password when prompted
```

### Example `.env` File (for Application Code)
```
MYSQLHOST=shuttle.proxy.rlwy.net
MYSQLPORT=24339
MYSQLDATABASE=trivia_app
MYSQLUSER=<your_team_username>
MYSQLPASSWORD=<your_team_password>
```
> ⚠️ Do **not** commit `.env` files or real passwords to GitHub.

---

## 👥 3. Team Accounts

| Team | Username   | Privileges |
|------|------------|-------------|
| **Backend Team**  | `backend_team` | Full read/write on `trivia_app.*` |
| **Frontend Team** | `frontend_team` | Full read/write on `trivia_app.*` |
| **QA Team**       | `qa_team` | Full read/write on `trivia_app.*` |
| **Database Team** | (Private) | Admin privileges & user management |

> All accounts are created and managed by the DB team.  
> Each team received its own credentials securely.

---

## 🧠 4. Database Schema Overview

| Table | Description |
|--------|--------------|
| **roles** | Defines roles (user, staff, admin). |
| **users** | Stores usernames, emails, and hashed passwords. |
| **categories** | Trivia question categories. |
| **questions** | Stores trivia questions (JSON answer options, correct key, YouTube/XML fields). |
| **individual_answers** | Individual player responses and scores. |
| **sessions** | Hosted trivia sessions metadata. |
| **moderated_answers** | Player responses during hosted sessions. |

### Schema Highlights
- All primary keys are `INT AUTO_INCREMENT`.
- Foreign keys enforce referential integrity with cascade updates.
- Timestamps use UTC and update automatically.
- Default roles:  
  - `100` → user  
  - `200` → staff  
  - `300` → admin
- JSON column `answers_option` stores choices in the format:
  ```json
  [
    {"key": "A", "text": "Option A"},
    {"key": "B", "text": "Option B"},
    {"key": "C", "text": "Option C"},
    {"key": "D", "text": "Option D"}
  ]
  ```

---

## 🧰 5. Developer Workflow

| Team | Responsibilities |
|------|------------------|
| **Database Team** | Maintain schema.sql and seed.sql, manage DB accounts, and run migrations. |
| **Backend Team**  | Build REST APIs that connect to the database. Use `.env` variables for DB credentials. |
| **Frontend Team** | Access trivia data through backend endpoints only. |
| **QA Team** | Run tests, verify queries, and validate data changes. |

---

## 🧾 6. Common Queries for Testing

```sql
USE trivia_app;

-- View all tables
SHOW TABLES;

-- View sample categories
SELECT * FROM categories;

-- Get recent questions
SELECT question_id, question_text FROM questions ORDER BY created_at DESC LIMIT 5;

-- Insert a test answer
INSERT INTO individual_answers (question_id, user_id, selected_answer, is_correct, score)
VALUES (1, 1, 'B', 1, 10);
```

---

## ⚙️ 7. Maintenance Commands (DB Team Only)

**Check users:**
```sql
SELECT user, host FROM mysql.user WHERE user IN ('backend_team','frontend_team','qa_team');
```

**Show team grants:**
```sql
SHOW GRANTS FOR 'backend_team'@'%';
SHOW GRANTS FOR 'frontend_team'@'%';
SHOW GRANTS FOR 'qa_team'@'%';
```

**Rotate a password:**
```sql
ALTER USER 'backend_team'@'%' IDENTIFIED BY 'NewPass!123';
FLUSH PRIVILEGES;
```

**Lock an account temporarily:**
```sql
ALTER USER 'qa_team'@'%' ACCOUNT LOCK;
ALTER USER 'qa_team'@'%' ACCOUNT UNLOCK;
```

---

## 🚦 8. Rules & Best Practices

✅ **Do:**
- Connect with SSL enabled.
- Use your own assigned account.
- Keep `.env` and passwords private.
- Communicate schema change requests through the DB team.

🚫 **Don’t:**
- Drop or alter tables directly.
- Share passwords publicly or in code.
- Use `root` or DB admin credentials.

---

## 🧑‍💻 9. Support & Contacts

| Name | Role |
|------|------|
| Haider  | DB Developer |
| Haven   | DB Developer |
| Jessie  | DB Developer |


If you encounter connection issues, share your Workbench screenshot (mask your password) and error message with the DB Team.

---

## ✅ Summary

- The entire class uses **one shared Railway MySQL database**.  
- Each team has its own MySQL account with full read/write privileges.  
- Use **SSL connections** and **.env** variables for security.  
- Schema management stays centralized with the **Database Team**.

---

*Prepared by:*  
**DataBase Team**  
COMP 3940 — Client Server Project, Fall 2025
