package com.payflowx.automation.config;

import java.io.InputStream;
import java.util.Properties;

public final class FrameworkConfig {
    private static final FrameworkConfig INSTANCE = new FrameworkConfig();
    private final Properties properties = new Properties();

    private FrameworkConfig() {
        String env = System.getProperty("env", System.getenv().getOrDefault("PAYFLOWX_ENV", "local"));
        String resource = "config/" + env + ".properties";
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing environment config: " + resource);
            }
            properties.load(inputStream);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load framework configuration", ex);
        }
    }

    public static FrameworkConfig get() {
        return INSTANCE;
    }

    public String baseUri() {
        return override("PAYFLOWX_BASE_URI", "base.uri");
    }

    public String accessToken() {
        return override("PAYFLOWX_ACCESS_TOKEN", "access.token");
    }

    public String signingSecret() {
        return override("PAYFLOWX_SIGNING_SECRET", "signing.secret");
    }

    public String dbUrl() {
        return override("DB_URL", "db.url");
    }

    public String dbUsername() {
        return override("DB_USERNAME", "db.username");
    }

    public String dbPassword() {
        return override("DB_PASSWORD", "db.password");
    }

    public String providerPin() {
        return override("PAYFLOWX_PROVIDER_PIN", "provider.pin");
    }

    private String override(String env, String key) {
        return System.getenv().getOrDefault(env, System.getProperty(key, properties.getProperty(key)));
    }
}
