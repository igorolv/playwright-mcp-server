package ru.it_spectrum.ai.playwright.mcp.testsupport;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class LocalTestSite implements AutoCloseable {

    private final HttpServer server;

    private LocalTestSite(HttpServer server) {
        this.server = server;
    }

    public static LocalTestSite start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            LocalTestSite site = new LocalTestSite(server);
            server.createContext("/", site::handle);
            server.start();
            return site;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start local test site", e);
        }
    }

    public String url(String path) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        return "http://127.0.0.1:" + server.getAddress().getPort() + normalized;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String body = switch (path) {
            case "/", "/index.html" -> indexHtml();
            case "/form.html" -> formHtml();
            case "/dynamic.html" -> dynamicHtml();
            case "/aria.html" -> ariaHtml();
            default -> notFoundHtml(path);
        };
        int status = body.startsWith("<!doctype html><title>Not found") ? 404 : 200;
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String indexHtml() {
        return """
                <!doctype html>
                <html lang="en">
                <head><title>Playwright MCP Test Site</title></head>
                <body>
                  <main>
                    <h1>Playwright MCP Test Site</h1>
                    <nav aria-label="Primary">
                      <a id="form-link" href="/form.html">Form page</a>
                      <a id="aria-link" href="/aria.html">ARIA page</a>
                    </nav>
                    <button id="hello" aria-label="Say hello">Hello</button>
                  </main>
                </body>
                </html>
                """;
    }

    private String formHtml() {
        return """
                <!doctype html>
                <html lang="en">
                <head><title>Form Page</title></head>
                <body>
                  <main>
                    <h1>Account Form</h1>
                    <label for="name">Name</label>
                    <input id="name" name="name" placeholder="Name">
                    <label><input id="active" type="checkbox"> Active</label>
                    <button id="save" type="button">Save</button>
                    <p id="status" aria-live="polite">Waiting</p>
                  </main>
                  <script>
                    document.getElementById('save').addEventListener('click', () => {
                      document.getElementById('status').textContent =
                        'Saved ' + document.getElementById('name').value;
                    });
                  </script>
                </body>
                </html>
                """;
    }

    private String dynamicHtml() {
        return """
                <!doctype html>
                <html lang="en">
                <head><title>Dynamic Page</title></head>
                <body>
                  <main>
                    <h1>Dynamic Page</h1>
                    <button id="add" type="button">Add item</button>
                    <section id="items" aria-label="Items"></section>
                  </main>
                  <script>
                    document.getElementById('add').addEventListener('click', () => {
                      const item = document.createElement('button');
                      item.id = 'created';
                      item.textContent = 'Created item';
                      document.getElementById('items').appendChild(item);
                    });
                  </script>
                </body>
                </html>
                """;
    }

    private String ariaHtml() {
        return """
                <!doctype html>
                <html lang="en">
                <head><title>ARIA Page</title></head>
                <body>
                  <main>
                    <h1 id="main-title">ARIA Examples</h1>
                    <section aria-labelledby="main-title">
                      <button id="danger" aria-label="Delete account">X</button>
                      <div id="custom" role="button" tabindex="0" aria-label="Custom action">Custom</div>
                    </section>
                  </main>
                </body>
                </html>
                """;
    }

    private String notFoundHtml(String path) {
        return "<!doctype html><title>Not found</title><h1>Not found</h1><p>" + path + "</p>";
    }
}
