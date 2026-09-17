package dev.local.windyfix;

import java.net.URI;

/** Keep the hook confined to the real HTTPS tile origin, without query-string matching. */
public final class TilePolicy {
    public static final String TARGET_PACKAGE = "com.lolaage.tbulu.tools";
    public static final String REFERER = "https://www.windy.com/";

    private TilePolicy() {}

    public static boolean matches(String value) {
        if (value == null) return false;
        try {
            URI uri = new URI(value);
            String path = uri.getRawPath();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "tiles.windy.com".equalsIgnoreCase(uri.getHost())
                    && uri.getRawUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443)
                    && path != null && path.startsWith("/v1/maptiles/");
        } catch (Exception ignored) {
            return false;
        }
    }
}
