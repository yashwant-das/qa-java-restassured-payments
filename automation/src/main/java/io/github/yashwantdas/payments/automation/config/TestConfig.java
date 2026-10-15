package io.github.yashwantdas.payments.automation.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Test settings. Each value comes from a system property, then its environment variable,
 * then {@code config.properties} on the test classpath.
 */
public final class TestConfig {
    private static final Properties FILE = load("config.properties");

    private TestConfig() {
    }

    public static String baseUri() {
        return get("base.uri", "PAYMENTS_BASE_URI");
    }

    public static String apiKey() {
        return get("api.key", "PAYMENTS_API_KEY");
    }

    public static String signingSecret() {
        return get("signing.secret", "PAYMENTS_SIGNING_SECRET");
    }

    public static String providerPin() {
        return get("provider.pin", "PAYMENTS_PROVIDER_PIN");
    }

    public static String dbUrl() {
        return get("db.url", "DB_URL");
    }

    public static String dbUsername() {
        return get("db.username", "DB_USERNAME");
    }

    public static String dbPassword() {
        return get("db.password", "DB_PASSWORD");
    }

    private static String get(String key, String envVar) {
        String value = System.getProperty(key, System.getenv(envVar));
        if (value == null) {
            value = FILE.getProperty(key);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing setting " + key + " (or environment variable " + envVar + ")");
        }
        return value;
    }

    private static Properties load(String resource) {
        Properties properties = new Properties();
        try (InputStream in = TestConfig.class.getClassLoader().getResourceAsStream(resource)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read " + resource, ex);
        }
        return properties;
    }
}
