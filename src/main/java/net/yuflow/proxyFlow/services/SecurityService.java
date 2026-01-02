package net.yuflow.proxyFlow.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.yuflow.proxyFlow.config.ConfigManager;
import org.slf4j.Logger;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SecurityService {
    private final ConfigManager configManager;
    private final Logger logger;
    private final Map<String, CacheEntry> ipCache;

    public SecurityService(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
        this.ipCache = new ConcurrentHashMap<>();
    }

    public static class VpnCheckResult {
        public boolean isVpn;
        public boolean isHosting;
        public String countryCode;

        public VpnCheckResult(boolean isVpn, boolean isHosting, String countryCode) {
            this.isVpn = isVpn;
            this.isHosting = isHosting;
            this.countryCode = countryCode;
        }
    }

    private static class CacheEntry {
        VpnCheckResult result;
        long timestamp;

        CacheEntry(VpnCheckResult result) {
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public VpnCheckResult checkIp(String ipAddress) {
        if (this.ipCache.containsKey(ipAddress)) {
            CacheEntry entry = this.ipCache.get(ipAddress);
            long cacheDurationMillis = TimeUnit.MINUTES.toMillis(configManager.getVpnCacheDuration());
            if (System.currentTimeMillis() - entry.timestamp < cacheDurationMillis) {
                return entry.result;
            }
            this.ipCache.remove(ipAddress);
        }

        VpnCheckResult result = performApiCheck(ipAddress);
        if (result != null) {
            this.ipCache.put(ipAddress, new CacheEntry(result));
        }
        return result;
    }

    private VpnCheckResult performApiCheck(String ipAddress) {
        String apiKey = configManager.getVpnCheckApiKey();
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("API KEY")) {
            return new VpnCheckResult(false, false, null);
        }

        try {
            URL url = new URL("https://proxycheck.io/v2/" + ipAddress + "?key=" + apiKey + "&vpn=1");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);

            if (connection.getResponseCode() == 200) {
                JsonObject response = new Gson().fromJson(new InputStreamReader(connection.getInputStream()), JsonObject.class);
                if (response.has(ipAddress)) {
                    JsonObject ipInfo = response.getAsJsonObject(ipAddress);
                    boolean isVpn = ipInfo.has("proxy") && "yes".equals(ipInfo.get("proxy").getAsString());
                    String type = ipInfo.has("type") ? ipInfo.get("type").getAsString() : "unknown";
                    boolean isHosting = "hosting".equalsIgnoreCase(type);
                    String country = ipInfo.has("isocode") ? ipInfo.get("isocode").getAsString() : null;

                    return new VpnCheckResult(isVpn, isHosting, country);
                }
            }
        } catch (Exception e) {
            logger.warn("VPN API check failed for IP " + ipAddress, e);
        }
        return new VpnCheckResult(false, false, null);
    }
}