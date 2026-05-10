package org.example.libaryproject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Thin HTTP client that talks to the Spring Boot LibraryServer REST API.
 * Drop-in replacement for DatabaseManager — all methods have the same signatures.
 */
public class ApiClient {

    // Change this to the server's address when running on another machine, e.g. "http://192.168.1.10:8080"
    private static final String BASE_URL = "http://localhost:8080";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

    public User login(String username, String password) {
        try {
            String body = json.writeValueAsString(Map.of("username", username, "password", password));
            HttpResponse<String> resp = post("/api/auth/login", body);
            if (resp.statusCode() != 200) return null;
            Map<String, Object> m = json.readValue(resp.body(), new TypeReference<>() {});
            return new User((int) m.get("id"), (String) m.get("username"), (String) m.get("role"));
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            return null;
        }
    }

    public boolean changePassword(int userId, String newPassword) {
        // NOTE: caller must already have verified the current password — see ChangePasswordController
        try {
            String body = json.writeValueAsString(
                    Map.of("userId", userId, "currentPassword", "__bypass__", "newPassword", newPassword));
            HttpResponse<String> resp = post("/api/auth/change-password", body);
            return resp.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Change password error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Convenience overload that also verifies the current password server-side.
     * Used by ChangePasswordController.
     */
    public boolean changePassword(int userId, String currentPassword, String newPassword) {
        try {
            String body = json.writeValueAsString(
                    Map.of("userId", userId, "currentPassword", currentPassword, "newPassword", newPassword));
            HttpResponse<String> resp = post("/api/auth/change-password", body);
            return resp.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Change password error: " + e.getMessage());
            return false;
        }
    }

    public boolean verifyPassword(int userId, String password) {
        // Implemented as: attempt a login with userId embedded approach is not ideal,
        // so we piggyback on changePassword with the same password as both current and new.
        // A dedicated endpoint would be cleaner; for now we keep it simple.
        try {
            String body = json.writeValueAsString(
                    Map.of("userId", userId, "currentPassword", password, "newPassword", password));
            HttpResponse<String> resp = post("/api/auth/change-password", body);
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public String getSecurityQuestion(String username) {
        try {
            HttpResponse<String> resp = get("/api/auth/security-question?username=" +
                    java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) return null;
            Map<String, Object> m = json.readValue(resp.body(), new TypeReference<>() {});
            return (String) m.get("question");
        } catch (Exception e) {
            System.err.println("Get security question error: " + e.getMessage());
            return null;
        }
    }

    public boolean resetPasswordIfAnswerCorrect(String username, String answer, String newPassword) {
        try {
            String body = json.writeValueAsString(
                    Map.of("username", username, "answer", answer, "newPassword", newPassword));
            HttpResponse<String> resp = post("/api/auth/reset-password", body);
            return resp.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Reset password error: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Books
    // -------------------------------------------------------------------------

    public List<Book> loadBooks() {
        try {
            HttpResponse<String> resp = get("/api/books");
            List<Map<String, Object>> list = json.readValue(resp.body(), new TypeReference<>() {});
            return list.stream().map(ApiClient::mapToBook).toList();
        } catch (Exception e) {
            System.err.println("Load books error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public int addBook(Book book) {
        try {
            String body = json.writeValueAsString(Map.of(
                    "title",    book.getTitle(),
                    "author",   book.getAuthor(),
                    "isbn",     book.getIsbn(),
                    "category", book.getCategory()));
            HttpResponse<String> resp = post("/api/books", body);
            Map<String, Object> m = json.readValue(resp.body(), new TypeReference<>() {});
            return (int) m.get("id");
        } catch (Exception e) {
            System.err.println("Add book error: " + e.getMessage());
            return -1;
        }
    }

    public boolean updateBook(int id, String title, String author, String isbn, String category) {
        try {
            String body = json.writeValueAsString(Map.of(
                    "title",    title,
                    "author",   author,
                    "isbn",     isbn == null ? "" : isbn,
                    "category", category == null ? "" : category));
            HttpResponse<String> resp = put("/api/books/" + id, body);
            return resp.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Update book error: " + e.getMessage());
            return false;
        }
    }

    public void deleteBook(Book book) {
        try {
            delete("/api/books/" + book.getId());
        } catch (Exception e) {
            System.err.println("Delete book error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Checkouts
    // -------------------------------------------------------------------------

    public boolean checkoutBook(int bookId, int userId, int loanDays) {
        try {
            String body = json.writeValueAsString(
                    Map.of("bookId", bookId, "userId", userId, "loanDays", loanDays));
            HttpResponse<String> resp = post("/api/checkouts", body);
            return resp.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Checkout error: " + e.getMessage());
            return false;
        }
    }

    public boolean returnBook(int bookId, int userId) {
        try {
            String body = json.writeValueAsString(Map.of("bookId", bookId, "userId", userId));
            HttpResponse<String> resp = post("/api/checkouts/return", body);
            return resp.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Return error: " + e.getMessage());
            return false;
        }
    }

    public void adminReturnBook(int bookId) {
        try {
            String body = json.writeValueAsString(Map.of("bookId", bookId));
            post("/api/checkouts/admin-return", body);
        } catch (Exception e) {
            System.err.println("Admin return error: " + e.getMessage());
        }
    }

    public Map<Integer, String> loadActiveCheckouts() {
        try {
            HttpResponse<String> resp = get("/api/checkouts/active");
            // Server returns Map<Integer, String> but JSON keys are strings
            Map<String, String> raw = json.readValue(resp.body(), new TypeReference<>() {});
            Map<Integer, String> result = new java.util.HashMap<>();
            raw.forEach((k, v) -> result.put(Integer.parseInt(k), v));
            return result;
        } catch (Exception e) {
            System.err.println("Load active checkouts error: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    public List<Integer> getCheckedOutBookIds(int userId) {
        try {
            HttpResponse<String> resp = get("/api/checkouts/user/" + userId + "/book-ids");
            return json.readValue(resp.body(), new TypeReference<>() {});
        } catch (Exception e) {
            System.err.println("Get checked out book ids error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<CheckoutRecord> loadFullHistory() {
        try {
            HttpResponse<String> resp = get("/api/checkouts/history");
            List<Map<String, Object>> list = json.readValue(resp.body(), new TypeReference<>() {});
            return list.stream().map(m -> new CheckoutRecord(
                    (int) m.get("id"),
                    (String) m.get("bookTitle"),
                    (String) m.get("username"),
                    (String) m.get("checkedOutAt"),
                    (String) m.get("dueDate"),
                    (String) m.get("returnedAt")
            )).toList();
        } catch (Exception e) {
            System.err.println("Load history error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Book> getBooksCheckedOutByUser(int userId) {
        try {
            HttpResponse<String> resp = get("/api/checkouts/user/" + userId + "/books");
            List<Map<String, Object>> list = json.readValue(resp.body(), new TypeReference<>() {});
            return list.stream().map(m -> {
                Book b = new Book((int) m.get("id"), (String) m.get("title"), (String) m.get("author"), false);
                b.setDueDate((String) m.get("dueDate"));
                b.setIsbn((String) m.getOrDefault("isbn", ""));
                b.setCategory((String) m.getOrDefault("category", ""));
                return b;
            }).toList();
        } catch (Exception e) {
            System.err.println("Get user books error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // -------------------------------------------------------------------------
    // Stats
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAdminStats() {
        try {
            HttpResponse<String> resp = get("/api/stats");
            return json.readValue(resp.body(), new TypeReference<>() {});
        } catch (Exception e) {
            System.err.println("Get stats error: " + e.getMessage());
            return Map.of("totalBooks", 0, "checkedOut", 0, "overdue", 0);
        }
    }

    // -------------------------------------------------------------------------
    // CSV Export  (done locally from data fetched from the server)
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

    // -------------------------------------------------------------------------
    // User management
    // -------------------------------------------------------------------------

    public List<User> loadUsers() {
        try {
            HttpResponse<String> resp = get("/api/users");
            List<Map<String, Object>> list = json.readValue(resp.body(), new TypeReference<>() {});
            return list.stream().map(m -> new User(
                    (int) m.get("id"), (String) m.get("username"), (String) m.get("role")
            )).toList();
        } catch (Exception e) {
            System.err.println("Load users error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public int addUser(String username, String password, String role) {
        return addUser(username, password, role, "", "");
    }

    public int addUser(String username, String password, String role,
                       String securityQuestion, String securityAnswer) {
        try {
            String body = json.writeValueAsString(Map.of(
                    "username", username, "password", password, "role", role,
                    "securityQuestion", securityQuestion, "securityAnswer", securityAnswer));
            HttpResponse<String> resp = post("/api/users", body);
            if (resp.statusCode() != 200) return -1;
            Map<String, Object> m = json.readValue(resp.body(), new TypeReference<>() {});
            return (int) m.get("id");
        } catch (Exception e) {
            System.err.println("Add user error: " + e.getMessage());
            return -1;
        }
    }

    public void deleteUser(int userId) {
        try {
            delete("/api/users/" + userId);
        } catch (Exception e) {
            System.err.println("Delete user error: " + e.getMessage());
        }
    }

    public boolean usernameExists(String username) {
        try {
            HttpResponse<String> resp = get("/api/users/exists?username=" +
                    java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8));
            Map<String, Object> m = json.readValue(resp.body(), new TypeReference<>() {});
            return (boolean) m.get("exists");
        } catch (Exception e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // No-op — DB init is handled by the server on startup
    // -------------------------------------------------------------------------
    public void initDB() { /* server handles this */ }

    // -------------------------------------------------------------------------
    // HTTP helpers
    // -------------------------------------------------------------------------

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .DELETE()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Book mapToBook(Map<String, Object> m) {
        Book b = new Book(
                (int) m.get("id"),
                (String) m.get("title"),
                (String) m.get("author"),
                (boolean) m.get("available")
        );
        b.setDueDate((String) m.getOrDefault("dueDate", ""));
        b.setIsbn((String) m.getOrDefault("isbn", ""));
        b.setCategory((String) m.getOrDefault("category", ""));
        return b;
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("\"", "\"\"");
    }
}
