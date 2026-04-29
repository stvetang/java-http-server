# Java HTTP Web Server

A lightweight, multithreaded HTTP web server built from scratch in Java using raw sockets. Supports serving static files and handling basic HTTP requests.

## Features

- **Multithreaded** — each incoming connection is handled on its own thread, allowing concurrent requests
- **GET requests** — serves static files (HTML, CSS, JS, images) from the local directory
- **POST requests** — reads and echoes back submitted body data
- **MIME type detection** — automatically sets `Content-Type` based on file extension
- **Path traversal protection** — blocks requests containing `..` to prevent directory escape
- **Standard HTTP error responses** — returns proper 400, 403, 404, and 405 status codes

## Supported File Types

| Extension | Content-Type |
|---|---|
| `.html` | `text/html` |
| `.css` | `text/css` |
| `.js` | `application/javascript` |
| `.png` | `image/png` |
| `.jpg` / `.jpeg` | `image/jpeg` |
| `.gif` | `image/gif` |
| Other | `application/octet-stream` |

## Getting Started

### Prerequisites

- Java 8 or later

### Running the Server

1. Compile the source file:
```bash
   javac WebServer.java
```

2. Run the server:
```bash
   java WebServer
```

The server will start on **port 6789** by default. Place any files you want to serve in the same directory you run the server from.

3. Open your browser and navigate to:
```bash
  http://localhost:6789/
```

## Project Structure
.
├── WebServer.java       # Main server entry point + request handler
└── index.html          # (optional) Default file served at /

## How It Works

1. `WebServer` opens a `ServerSocket` on port 6789 and listens for connections in a loop.
2. Each accepted connection is handed off to an `HttpRequestHandler` running on a new thread.
3. The handler manually parses the raw HTTP request — reading headers byte-by-byte until the `\r\n\r\n` header terminator is found.
4. Based on the HTTP method (`GET` or `POST`), the appropriate logic is executed.
5. For `GET` requests, the file is located on disk and streamed back with the correct headers.
