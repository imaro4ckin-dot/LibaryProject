package org.example.libaryproject;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:library.db";

    private Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA busy_timeout=5000");
        }
        return conn;
    }

    public void initDB() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS books (
                        id        INTEGER PRIMARY KEY AUTOINCREMENT,
                        title     TEXT    NOT NULL,
                        author    TEXT    NOT NULL,
                        available INTEGER NOT NULL DEFAULT 1
                    );
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id       INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT    NOT NULL UNIQUE,
                        password TEXT    NOT NULL,
                        role     TEXT    NOT NULL
                    );
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS checkouts (
                        id              INTEGER PRIMARY KEY AUTOINCREMENT,
                        book_id         INTEGER NOT NULL,
                        user_id         INTEGER NOT NULL,
                        checked_out_at  TEXT    NOT NULL,
                        due_date        TEXT    NOT NULL,
                        returned_at     TEXT,
                        FOREIGN KEY (book_id) REFERENCES books(id),
                        FOREIGN KEY (user_id) REFERENCES users(id)
                    );
                    """);
            // Migrate: add due_date column if upgrading from old schema
            try {
                stmt.execute("ALTER TABLE checkouts ADD COLUMN due_date TEXT NOT NULL DEFAULT ''");
            } catch (SQLException ignored) { /* column already exists */ }

            // Migrate: add security question columns if upgrading from old schema
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN security_question TEXT NOT NULL DEFAULT ''");
                stmt.execute("ALTER TABLE users ADD COLUMN security_answer TEXT NOT NULL DEFAULT ''");
            } catch (SQLException ignored) { /* columns already exist */ }

            // Seed default accounts if empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO users (username, password, role) VALUES ('admin', 'admin123', 'admin')");
                stmt.execute("INSERT INTO users (username, password, role) VALUES ('user',  'user123',  'user')");
            }
        } catch (SQLException e) {
            System.err.println("Error initialising database: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

    public User login(String username, String password) {
        String sql = "SELECT id, username, role FROM users WHERE username = ? AND password = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return new User(rs.getInt("id"), rs.getString("username"), rs.getString("role"));
            }
        } catch (SQLException e) {
            System.err.println("Error during login: " + e.getMessage());
        }
        return null;
    }

    public boolean changePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error changing password: " + e.getMessage());
        }
        return false;
    }

    public boolean verifyPassword(int userId, String password) {
        String sql = "SELECT 1 FROM users WHERE id = ? AND password = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error verifying password: " + e.getMessage());
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Books
    // -------------------------------------------------------------------------

    public List<Book> loadBooks() {
        List<Book> books = new ArrayList<>();
        // Also pull the due_date from any active checkout
        String sql = """
                SELECT b.id, b.title, b.author, b.available, c.due_date
                FROM books b
                LEFT JOIN checkouts c ON c.book_id = b.id AND c.returned_at IS NULL
                ORDER BY b.id
                """;
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Book book = new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("available") == 1
                );
                book.setDueDate(rs.getString("due_date"));
                books.add(book);
            }
        } catch (SQLException e) {
            System.err.println("Error loading books: " + e.getMessage());
        }
        return books;
    }

    public int addBook(Book book) {
        String sql = "INSERT INTO books (title, author, available) VALUES (?, ?, 1)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error adding book: " + e.getMessage());
        }
        return -1;
    }

    public boolean updateBook(int id, String title, String author) {
        String sql = "UPDATE books SET title = ?, author = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating book: " + e.getMessage());
        }
        return false;
    }

    public void deleteBook(Book book) {
        try (Connection conn = connect()) {
            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM checkouts WHERE book_id = ?");
            ps1.setInt(1, book.getId());
            ps1.executeUpdate();
            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM books WHERE id = ?");
            ps2.setInt(1, book.getId());
            ps2.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting book: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Checkouts
    // -------------------------------------------------------------------------

    public boolean checkoutBook(int bookId, int userId, int loanDays) {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            PreparedStatement check = conn.prepareStatement(
                    "SELECT available FROM books WHERE id = ?");
            check.setInt(1, bookId);
            ResultSet rs = check.executeQuery();
            if (!rs.next() || rs.getInt("available") == 0) {
                conn.rollback();
                return false;
            }
            PreparedStatement mark = conn.prepareStatement(
                    "UPDATE books SET available = 0 WHERE id = ?");
            mark.setInt(1, bookId);
            mark.executeUpdate();

            PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO checkouts (book_id, user_id, checked_out_at, due_date) " +
                    "VALUES (?, ?, datetime('now'), date('now', '+' || ? || ' days'))");
            insert.setInt(1, bookId);
            insert.setInt(2, userId);
            insert.setInt(3, loanDays);
            insert.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error checking out book: " + e.getMessage());
            return false;
        }
    }

    public boolean returnBook(int bookId, int userId) {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            PreparedStatement find = conn.prepareStatement("""
                    SELECT id FROM checkouts
                    WHERE book_id = ? AND user_id = ? AND returned_at IS NULL
                    """);
            find.setInt(1, bookId);
            find.setInt(2, userId);
            ResultSet rs = find.executeQuery();
            if (!rs.next()) { conn.rollback(); return false; }
            int checkoutId = rs.getInt("id");

            PreparedStatement close = conn.prepareStatement(
                    "UPDATE checkouts SET returned_at = datetime('now') WHERE id = ?");
            close.setInt(1, checkoutId);
            close.executeUpdate();

            PreparedStatement markAvailable = conn.prepareStatement(
                    "UPDATE books SET available = 1 WHERE id = ?");
            markAvailable.setInt(1, bookId);
            markAvailable.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error returning book: " + e.getMessage());
            return false;
        }
    }

    public void adminReturnBook(int bookId) {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            PreparedStatement close = conn.prepareStatement(
                    "UPDATE checkouts SET returned_at = datetime('now') WHERE book_id = ? AND returned_at IS NULL");
            close.setInt(1, bookId);
            close.executeUpdate();
            PreparedStatement mark = conn.prepareStatement(
                    "UPDATE books SET available = 1 WHERE id = ?");
            mark.setInt(1, bookId);
            mark.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Error force-returning book: " + e.getMessage());
        }
    }

    public Map<Integer, String> loadActiveCheckouts() {
        Map<Integer, String> map = new HashMap<>();
        String sql = """
                SELECT c.book_id, u.username
                FROM checkouts c JOIN users u ON u.id = c.user_id
                WHERE c.returned_at IS NULL
                """;
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                map.put(rs.getInt("book_id"), rs.getString("username"));
        } catch (SQLException e) {
            System.err.println("Error loading active checkouts: " + e.getMessage());
        }
        return map;
    }

    public List<Integer> getCheckedOutBookIds(int userId) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT book_id FROM checkouts WHERE user_id = ? AND returned_at IS NULL";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("book_id"));
        } catch (SQLException e) {
            System.err.println("Error loading user checkouts: " + e.getMessage());
        }
        return ids;
    }

    public List<CheckoutRecord> loadFullHistory() {
        List<CheckoutRecord> records = new ArrayList<>();
        String sql = """
                SELECT c.id, b.title, u.username, c.checked_out_at, c.due_date, c.returned_at
                FROM checkouts c
                JOIN books b ON b.id = c.book_id
                JOIN users u ON u.id = c.user_id
                ORDER BY c.checked_out_at DESC
                """;
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                records.add(new CheckoutRecord(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("username"),
                        rs.getString("checked_out_at"),
                        rs.getString("due_date"),
                        rs.getString("returned_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading history: " + e.getMessage());
        }
        return records;
    }

    public List<Book> getBooksCheckedOutByUser(int userId) {
        List<Book> books = new ArrayList<>();
        String sql = """
                SELECT b.id, b.title, b.author, c.due_date
                FROM checkouts c JOIN books b ON b.id = c.book_id
                WHERE c.user_id = ? AND c.returned_at IS NULL
                ORDER BY c.due_date
                """;
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Book book = new Book(rs.getInt("id"), rs.getString("title"), rs.getString("author"), false);
                book.setDueDate(rs.getString("due_date"));
                books.add(book);
            }
        } catch (SQLException e) {
            System.err.println("Error loading user books: " + e.getMessage());
        }
        return books;
    }

    // -------------------------------------------------------------------------
    // CSV Export
    // -------------------------------------------------------------------------

    public void exportBooksCSV(String path) throws IOException {
        try (FileWriter fw = new FileWriter(path)) {
            fw.write("ID,Title,Author,Available,Due Date\n");
            for (Book b : loadBooks()) {
                fw.write(String.format("%d,\"%s\",\"%s\",%s,%s\n",
                        b.getId(), esc(b.getTitle()), esc(b.getAuthor()),
                        b.getAvailable() ? "Yes" : "No",
                        b.getDueDate() != null ? b.getDueDate() : ""));
            }
        }
    }

    public void exportHistoryCSV(String path) throws IOException {
        try (FileWriter fw = new FileWriter(path)) {
            fw.write("ID,Book,User,Checked Out At,Due Date,Returned At\n");
            for (CheckoutRecord r : loadFullHistory()) {
                fw.write(String.format("%d,\"%s\",\"%s\",%s,%s,%s\n",
                        r.getId(), esc(r.getBookTitle()), esc(r.getUsername()),
                        r.getCheckedOutAt(), r.getDueDate(), r.getReturnedAt()));
            }
        }
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("\"", "\"\"");
    }

    // -------------------------------------------------------------------------
    // User management
    // -------------------------------------------------------------------------

    public List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, role FROM users ORDER BY id";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                users.add(new User(rs.getInt("id"), rs.getString("username"), rs.getString("role")));
        } catch (SQLException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
        return users;
    }

    public int addUser(String username, String password, String role) {
        return addUser(username, password, role, "", "");
    }

    public int addUser(String username, String password, String role,
                       String securityQuestion, String securityAnswer) {
        String sql = "INSERT INTO users (username, password, role, security_question, security_answer) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setString(4, securityQuestion);
            ps.setString(5, securityAnswer.toLowerCase().trim());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error adding user: " + e.getMessage());
        }
        return -1;
    }

    public void deleteUser(int userId) {
        try (Connection conn = connect()) {
            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM checkouts WHERE user_id = ?");
            ps1.setInt(1, userId);
            ps1.executeUpdate();
            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM users WHERE id = ?");
            ps2.setInt(1, userId);
            ps2.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
        }
    }

    /** Returns the security question for a username, or null if user not found / no question set. */
    public String getSecurityQuestion(String username) {
        String sql = "SELECT security_question FROM users WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String q = rs.getString("security_question");
                return (q != null && !q.isBlank()) ? q : null;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching security question: " + e.getMessage());
        }
        return null;
    }

    /**
     * Verifies the security answer and resets the password if correct.
     * Answers are compared case-insensitively.
     * Returns true on success, false if username not found or answer wrong.
     */
    public boolean resetPasswordIfAnswerCorrect(String username, String answer, String newPassword) {
        // Read phase — fully closed before the write phase opens
        int userId = -1;
        String sql = "SELECT id, security_answer FROM users WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String stored = rs.getString("security_answer");
                if (stored == null || !stored.equalsIgnoreCase(answer.trim())) return false;
                userId = rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("Error resetting password: " + e.getMessage());
            return false;
        }
        // Write phase — separate connection, no lock conflict
        return changePassword(userId, newPassword);
    }

    public boolean usernameExists(String username) {        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.err.println("Error checking username: " + e.getMessage());
        }
        return false;
    }
}
