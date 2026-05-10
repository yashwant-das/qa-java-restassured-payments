package com.payflowx.automation.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public final class SignatureGenerator {
    private SignatureGenerator() {
    }

    public static String sign(String secret, String method, String path, String queryString, String timestamp) {
        String canonical = method + "\n" + path + "\n" + (queryString == null ? "" : queryString) + "\n" + timestamp;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign request", ex);
        }
    }
}
