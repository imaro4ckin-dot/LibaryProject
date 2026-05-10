# Library Management System

A JavaFX desktop application for managing a library with multi-user support, SQLite storage, and role-based access.

## Requirements

- Java 21+
- No other installation needed — Maven downloads all dependencies automatically

## Run

**Mac / Linux**
```bash
./mvnw javafx:run
```

**Windows**
```cmd
mvnw.cmd javafx:run
```

## Default accounts

| Username | Password  | Role  |
|----------|-----------|-------|
| admin    | admin123  | Admin |
| user     | user123   | User  |

New accounts can be created by an admin from the **Manage Users** screen.

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
