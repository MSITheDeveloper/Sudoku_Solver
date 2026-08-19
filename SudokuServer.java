import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class SudokuServer {

    private static final int PORT = 8080;
    private static final int GRID_SIZE = 9;
    private static final int BOX_SIZE = 3;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Route: Serve Web Page UI
        server.createContext("/", new StaticFileHandler());

        // Route: Solve API endpoint
        server.createContext("/api/solve", new SolveHandler());

        server.setExecutor(null);
        System.out.println("==================================================");
        System.out.println("Sudoku Solver Server running at http://localhost:" + PORT);
        System.out.println("==================================================");
        server.start();
    }

    // --- Core Algorithmic Solver Engine ---
    public static class SolverEngine {
        private final int[] rowMask = new int[GRID_SIZE];
        private final int[] colMask = new int[GRID_SIZE];
        private final int[] boxMask = new int[GRID_SIZE];

        public boolean solve(int[][] board) {
            for (int r = 0; r < GRID_SIZE; r++) {
                for (int c = 0; c < GRID_SIZE; c++) {
                    int val = board[r][c];
                    if (val != 0) {
                        int bit = 1 << val;
                        int b = (r / BOX_SIZE) * BOX_SIZE + (c / BOX_SIZE);

                        if ((rowMask[r] & bit) != 0 || (colMask[c] & bit) != 0 || (boxMask[b] & bit) != 0) {
                            return false; // Edge case: Conflict in initial board
                        }

                        rowMask[r] |= bit;
                        colMask[c] |= bit;
                        boxMask[b] |= bit;
                    }
                }
            }
            return backtrack(board, 0, 0);
        }

        private boolean backtrack(int[][] board, int r, int c) {
            if (r == GRID_SIZE) return true;

            int nextR = (c == GRID_SIZE - 1) ? r + 1 : r;
            int nextC = (c == GRID_SIZE - 1) ? 0 : c + 1;

            if (board[r][c] != 0) {
                return backtrack(board, nextR, nextC);
            }

            int b = (r / BOX_SIZE) * BOX_SIZE + (c / BOX_SIZE);

            for (int num = 1; num <= 9; num++) {
                int bit = 1 << num;

                // O(1) Bitwise constraint lookup
                if ((rowMask[r] & bit) == 0 && (colMask[c] & bit) == 0 && (boxMask[b] & bit) == 0) {
                    board[r][c] = num;
                    rowMask[r] |= bit;
                    colMask[c] |= bit;
                    boxMask[b] |= bit;

                    if (backtrack(board, nextR, nextC)) return true;

                    board[r][c] = 0;
                    rowMask[r] &= ~bit;
                    colMask[c] &= ~bit;
                    boxMask[b] &= ~bit;
                }
            }
            return false;
        }
    }

    // --- Static Frontend Handler ---
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File file = new File("index.html");
            if (!file.exists()) {
                String response = "Error: index.html not found in working directory.";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            byte[] bytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(bytes);
            }

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // --- REST API Handler ---
    static class SolveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            int[][] board = parseGridFromJson(body);
            if (board == null) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid grid format\"}");
                return;
            }

            long startTime = System.nanoTime();
            SolverEngine solver = new SolverEngine();
            boolean solved = solver.solve(board);
            long durationMicros = (System.nanoTime() - startTime) / 1000;

            if (solved) {
                StringBuilder json = new StringBuilder();
                json.append("{\"success\":true,\"executionTimeUs\":").append(durationMicros).append(",\"grid\":[");
                for (int r = 0; r < 9; r++) {
                    json.append("[");
                    for (int c = 0; c < 9; c++) {
                        json.append(board[r][c]).append(c == 8 ? "" : ",");
                    }
                    json.append("]").append(r == 8 ? "" : ",");
                }
                json.append("]}");
                sendJsonResponse(exchange, 200, json.toString());
            } else {
                sendJsonResponse(exchange, 200, "{\"success\":false,\"error\":\"No valid solution exists for this configuration.\"}");
            }
        }

        private int[][] parseGridFromJson(String json) {
            try {
                int[][] grid = new int[9][9];
                String clean = json.replaceAll("[^0-9,]", "");
                String[] tokens = clean.split(",");
                if (tokens.length != 81) return null;

                for (int i = 0; i < 81; i++) {
                    grid[i / 9][i % 9] = Integer.parseInt(tokens[i].trim());
                }
                return grid;
            } catch (Exception e) {
                return null;
            }
        }

        private void sendJsonResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}