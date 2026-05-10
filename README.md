# Library Management System

A two-part application: a **Spring Boot REST server** that holds all data, and a **JavaFX desktop client** that connects to it. Multiple laptops can run the client simultaneously and stay in sync because they all talk to the same server.

```
LibraryServer/   ← Spring Boot (runs on one machine, port 8080)
LibaryProject/   ← JavaFX desktop app (runs on any number of machines)
```

## Requirements

- Java 21+
- No other installation needed — Maven downloads all dependencies automatically

---

## 1 — Start the server (run once, on one machine)

```bash
cd LibraryServer
./mvnw spring-boot:run
```

The server starts on **http://localhost:8080** and creates `library.db` in that folder on first run.

To let other laptops connect, find the server machine's local IP address (e.g. `192.168.1.10`) and update **one line** in the client before building it:

```
LibaryProject/src/main/java/org/example/libaryproject/ApiClient.java
  → private static final String BASE_URL = "http://192.168.1.10:8080";
```

---

## 2 — Start the client (any number of laptops)

**Mac / Linux**
```bash
cd LibaryProject
./mvnw javafx:run
```

**Windows**
```cmd
cd LibaryProject
mvnw.cmd javafx:run
```

---

## Default accounts

| Username | Password  | Role  |
|----------|-----------|-------|
| admin    | admin123  | Admin |
| user     | user123   | User  |

New accounts can be created by an admin from the **Manage Users** screen.

---

## Features

**Admin**
- Add, edit, delete books
- Checkout and force-return books
- Full checkout history with CSV export
- Manage users (create, delete, view checked-out books)

**User**
- Browse available books and check them out (choose loan duration)
- View and return your own books
- Overdue books highlighted in red

**Both roles**
- Change password
- Forgot password via security question
