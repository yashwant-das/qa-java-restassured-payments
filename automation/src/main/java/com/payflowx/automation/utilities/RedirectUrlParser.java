package com.payflowx.automation.utilities;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class RedirectUrlParser {
    private RedirectUrlParser() {
    }

    public static String providerHash(String redirectUrl) {
        String query = URI.create(redirectUrl).getRawQuery();
        return Arrays.stream(query.split("&"))
                .map(pair -> pair.split("=", 2))
                .filter(parts -> parts.length == 2 && parts[0].equals("providerHash"))
                .map(parts -> URLDecoder.decode(parts[1], StandardCharsets.UTF_8))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("providerHash missing from redirectURL"));
    }
}
