# Library Client

JavaFX desktop app for the Library Management System. Connects to the [Library Server](https://github.com/YOUR_USERNAME/LibraryServer) over HTTP — multiple laptops can run this client simultaneously and stay in sync.

## Requirements

- Java 21+
- [LibraryServer](https://github.com/YOUR_USERNAME/LibraryServer) running (on the same machine or your local network)

## Setup

**1. Start the server first** (see the [LibraryServer](https://github.com/YOUR_USERNAME/LibraryServer) repo).

**2. If the server is on a different machine**, open `src/main/java/org/example/libaryproject/ApiClient.java` and update the first line of the class:

```java
private static final String BASE_URL = "http://192.168.1.10:8080"; // ← server's IP
```

**3. Run the client:**

```bash
# Mac / Linux
./mvnw javafx:run

# Windows
mvnw.cmd javafx:run
```

No installation needed — Maven downloads all dependencies automatically.

## Default accounts

| Username | Password  | Role  |
|----------|-----------|-------|
| admin    | admin123  | Admin |
| user     | user123   | User  |

New accounts can be created by an admin from the **Manage Users** screen.

## Features

### Admin
- Add, edit, and delete books (with ISBN and category)
- Filter books by category; search by title, author, or ISBN
- Checkout and force-return books on behalf of users
- Dashboard showing total books, currently checked out, and overdue count
- Full checkout history with CSV export
- Manage users: create, delete, view their checked-out books

### User
- Browse available books — searchable by title, author, ISBN, or category
- Check out a book and choose the loan duration (1–4 weeks)
- View your checked-out books; return them when done
- Books due within 3 days are highlighted yellow; overdue books are highlighted red
- Maximum 5 books checked out at once
- Change password / reset password via security question
