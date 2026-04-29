import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;

public class WebServer {
    public static void main(String[] args) {
        int port = 6789;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("HTTP Server running on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                HttpRequestHandler requestHandler = new HttpRequestHandler(clientSocket);
                Thread thread = new Thread(requestHandler);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

class HttpRequestHandler implements Runnable {
    private final Socket socket;

    public HttpRequestHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            DataOutputStream out = new DataOutputStream(os)
        ) {
            ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();

            int prev = 0, curr;
            while ((curr = is.read()) != -1) {
                headerBuffer.write(curr);
                int len = headerBuffer.size();

                if (prev == '\r' && curr == '\n' && len >= 4) {
                    byte[] b = headerBuffer.toByteArray();
                    if (b[len - 4] == '\r' && b[len - 3] == '\n') {
                        break;
                    }
                }
                prev = curr;
            }

            String headersText = headerBuffer.toString();
            String[] lines = headersText.split("\r\n");

            if (lines.length == 0) {
                send400(out);
                return;
            }

            String[] requestLine = lines[0].split(" ");
            if (requestLine.length != 3) {
                send400(out);
                return;
            }

            String method = requestLine[0];
            String path = requestLine[1];

            Map<String, String> headers = new HashMap<>();
            for (int i = 1; i < lines.length; i++) {
                String[] headerParts = lines[i].split(":", 2);
                if (headerParts.length == 2) {
                    headers.put(headerParts[0].trim().toLowerCase(), headerParts[1].trim());
                }
            }

            if (method.equals("POST")) {
                if (!headers.containsKey("content-length")) {
                    send400(out);
                    return;
                }

                int contentLength = Integer.parseInt(headers.get("content-length"));
                byte[] bodyBytes = new byte[contentLength];

                int bytesRead = 0;
                while (bytesRead < contentLength) {
                    int read = is.read(bodyBytes, bytesRead, contentLength - bytesRead);
                    if (read == -1) break;
                    bytesRead += read;
                }

                String body = new String(bodyBytes, "UTF-8");
                String responseBody = "<html><body><h1>POST Data Received</h1><pre>" 
                    + body + "</pre></body></html>";

                byte[] responseBytes = responseBody.getBytes("UTF-8");

                out.writeBytes("HTTP/1.1 200 OK\r\n");
                out.writeBytes("Content-Type: text/html\r\n");
                out.writeBytes("Content-Length: " + responseBytes.length + "\r\n");
                out.writeBytes("\r\n");
                out.write(responseBytes);

            } else if (method.equals("GET")) {
                handleGet(path, out);
            } else {
                send405(out);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void handleGet(String path, DataOutputStream out) throws IOException {
        if (path.equals("/")) {
            path = "/index.html";
        }

        path = URLDecoder.decode(path, "UTF-8");

        if (path.contains("..")) {
            send403(out);
            return;
        }

        File file = new File("." + path);
        if (!file.exists() || !file.isFile()) {
            send404(out);
            return;
        }

        byte[] fileBytes = Files.readAllBytes(file.toPath());
        String contentType = getContentType(path);

        out.writeBytes("HTTP/1.1 200 OK\r\n");
        out.writeBytes("Content-Type: " + contentType + "\r\n");
        out.writeBytes("Content-Length: " + fileBytes.length + "\r\n");
        out.writeBytes("\r\n");
        out.write(fileBytes);
    }

    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        return "application/octet-stream";
    }

    private void sendResponse(DataOutputStream out, String status, String body) throws IOException {
        byte[] bodyBytes = body.getBytes("UTF-8");
        out.writeBytes("HTTP/1.1 " + status + "\r\n");
        out.writeBytes("Content-Type: text/html\r\n");
        out.writeBytes("Content-Length: " + bodyBytes.length + "\r\n");
        out.writeBytes("\r\n");
        out.write(bodyBytes);
    }

    private void send400(DataOutputStream out) throws IOException {
        sendResponse(out, "400 Bad Request", "<h1>400 Bad Request</h1>");
    }

    private void send403(DataOutputStream out) throws IOException {
        sendResponse(out, "403 Forbidden", "<h1>403 Forbidden</h1>");
    }

    private void send404(DataOutputStream out) throws IOException {
        sendResponse(out, "404 Not Found", "<h1>404 Not Found</h1>");
    }

    private void send405(DataOutputStream out) throws IOException {
        sendResponse(out, "405 Method Not Allowed", "<h1>405 Method Not Allowed</h1>");
    }
}
