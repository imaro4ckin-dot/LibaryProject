package org.example.libaryproject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:library.db";

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
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
            // Seed default accounts if the table is empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO users (username, password, role) VALUES ('admin', 'admin123', 'admin')");
                stmt.execute("INSERT INTO users (username, password, role) VALUES ('user', 'user123', 'user')");
            }
        } catch (SQLException e) {
            System.err.println("Error initialising database: " + e.getMessage());
        }
    }

    /** Returns the role ("admin" or "user") on success, or null on bad credentials. */
    public String login(String username, String password) {
        String sql = "SELECT role FROM users WHERE username = ? AND password = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("role");
            }
        } catch (SQLException e) {
            System.err.println("Error during login: " + e.getMessage());
        }
        return null;
    }

    public List<Book> loadBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT id, title, author, available FROM books ORDER BY id";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("available") == 1
                ));
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

    public void updateAvailability(Book book) {
        String sql = "UPDATE books SET available = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, book.getAvailable() ? 1 : 0);
            ps.setInt(2, book.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating book: " + e.getMessage());
        }
    }

    public void deleteBook(Book book) {
        String sql = "DELETE FROM books WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, book.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting book: " + e.getMessage());
        }
    }
}
