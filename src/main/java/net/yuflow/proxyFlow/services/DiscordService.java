package net.yuflow.proxyFlow.services;

import com.google.gson.JsonObject;
import net.yuflow.proxyFlow.config.ConfigManager;
import org.slf4j.Logger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class DiscordService {
    private final ConfigManager configManager;
    private final Logger logger;
    private final HttpClient httpClient;

    public DiscordService(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void sendNotification(String title, String description, int color) {
        if (!configManager.isDiscordEnabled() || configManager.getDiscordWebhookUrl().isEmpty()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                JsonObject json = new JsonObject();
                JsonObject embed = new JsonObject();
                embed.addProperty("title", title);
                embed.addProperty("description", description);
                embed.addProperty("color", color);

                com.google.gson.JsonArray embeds = new com.google.gson.JsonArray();
                embeds.add(embed);
                json.add("embeds", embeds);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(configManager.getDiscordWebhookUrl()))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                        .build();

                this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                logger.error("Failed to send Discord webhook", e);
            }
        });
    }
}