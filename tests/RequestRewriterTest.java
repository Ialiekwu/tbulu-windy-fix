import dev.local.windyfix.RequestRewriter;
import dev.local.windyfix.TilePolicy;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public final class RequestRewriterTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        String tile = "https://tiles.windy.com/v1/maptiles/outdoor/256%402x/3/1/2/?lang=en";
        check(TilePolicy.matches(tile), "encoded tile URL");
        check(TilePolicy.matches(tile.replace("%40", "@")), "literal tile URL");
        check(TilePolicy.matches(tile.replace(".com/", ".com:443/")), "explicit HTTPS port");
        String[] excluded = {null, "", "not a URL", "http://tiles.windy.com/v1/maptiles/a",
            "https://tiles.windy.com.evil.test/v1/maptiles/a", "https://tiles.windy.com@evil.test/v1/maptiles/a",
            "https://example.com/?url=" + tile, "https://tiles.windy.com/login", "https://api.2bulu.com/v1/maptiles/a",
            "https://tiles.windy.com:8443/v1/maptiles/a", "https://account@tiles.windy.com/v1/maptiles/a"};
        for (String url : excluded) check(!TilePolicy.matches(url), "excluded origin: " + url);
        RequestRewriter rewriter = new RequestRewriter(Request.class, Request.Builder.class);
        Object tag = new Object();
        RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), new byte[]{1,2,3});
        Request original = new Request.Builder().url(tile).header("X-Test", "preserved")
            .addHeader("Referer", "https://example.com/").addHeader("Referer", "https://wrong.example/")
            .post(body).tag(tag).build();
        Request fixed = (Request) rewriter.rewrite(original);
        check(fixed != original, "immutable request was copied");
        check(fixed.headers("Referer").size() == 1, "duplicates replaced");
        check(TilePolicy.REFERER.equals(fixed.header("Referer")), "correct Referer");
        check(fixed.url().equals(original.url()) && fixed.body() == body && fixed.tag() == tag,
            "URL, body and tag preserved");
        check("POST".equals(fixed.method()) && "preserved".equals(fixed.header("X-Test")), "method and headers preserved");
        check(original.headers("Referer").size() == 2, "input request untouched");
        check(rewriter.rewrite(fixed) == fixed, "idempotent");
        Request unrelated = new Request.Builder().url("https://example.com/v1/maptiles/a").build();
        check(rewriter.rewrite(unrelated) == unrelated, "non-Windy request stays identical");
        Request noHeader = new Request.Builder().url(tile).build();
        check(TilePolicy.REFERER.equals(((Request) rewriter.rewrite(noHeader)).header("Referer")), "missing header added");
        check(rewriter.rewrite(null) == null, "null left to original client validation");
        System.out.println("PASS: exact origin scope; header replacement; immutable request preservation; idempotence");
    }
}
