package com.company.telegram;

import com.company.config.Config;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Oddiy HTTP javob beruvchi: har qanday so'rovga "OK" deb javob qaytaradi.
 *
 * Ikkita ish uchun kerak:
 *  1) Render'ning bepul "Web Service" turi ilova $PORT portini eshitishini talab qiladi —
 *     aks holda xizmat ishga tushmagan deb hisoblanadi va o'chiriladi;
 *  2) bepul Render xizmati harakatsizlikdan uxlab qoladi, shuning uchun tashqaridan
 *     (masalan cron-job.org) har 10 daqiqada so'rov yuborib uyg'oq saqlanadi.
 */
public final class HealthServer {

    private HealthServer() { }

    public static void start() {
        try {
            // Render PORT ni o'zi beradi; lokalda HEALTH_PORT yoki 8080 ishlatiladi.
            int port = Integer.parseInt(Config.get("PORT", Config.get("HEALTH_PORT", "8080")).trim());
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/", HealthServer::ok);
            server.setExecutor(null);
            server.start();
            System.out.println("HealthServer 0.0.0.0:" + port + " da ishga tushdi (javob: OK).");
        } catch (Exception e) {
            // Bu server ishlamasa ham bot o'zi ishlashda davom etsin.
            System.out.println("HealthServer ishga tushmadi: " + e.getMessage());
        }
    }

    private static void ok(HttpExchange exchange) throws IOException {
        byte[] body = "OK".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
