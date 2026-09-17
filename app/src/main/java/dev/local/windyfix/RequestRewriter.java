package dev.local.windyfix;

import java.lang.reflect.Method;

/** Reflect into the app's OkHttp version; do not bundle another HTTP client. */
public final class RequestRewriter {
    private final Method url;
    private final Method header;
    private final Method newBuilder;
    private final Method setHeader;
    private final Method build;

    public RequestRewriter(Class<?> requestClass, Class<?> builderClass) throws ReflectiveOperationException {
        url = requestClass.getMethod("url");
        header = requestClass.getMethod("header", String.class);
        newBuilder = requestClass.getMethod("newBuilder");
        setHeader = builderClass.getMethod("header", String.class, String.class);
        build = builderClass.getMethod("build");
    }

    public boolean matches(Object request) throws ReflectiveOperationException {
        return request != null && TilePolicy.matches(String.valueOf(url.invoke(request)));
    }

    public Object rewrite(Object request) throws ReflectiveOperationException {
        if (!matches(request) || TilePolicy.REFERER.equals(header.invoke(request, "Referer"))) {
            return request;
        }
        Object builder = newBuilder.invoke(request);
        setHeader.invoke(builder, "Referer", TilePolicy.REFERER);
        return build.invoke(builder);
    }
}
