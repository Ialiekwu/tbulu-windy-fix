package dev.local.windyfix;

import android.app.Application;
import android.content.Context;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class WindyHook implements IXposedHookLoadPackage {
    private static final String TAG = "[WindyFix] ";
    private final Set<Class<?>> installed = Collections.newSetFromMap(new IdentityHashMap<Class<?>, Boolean>());
    private final AtomicInteger patched = new AtomicInteger();
    private final AtomicInteger responses = new AtomicInteger();
    private final AtomicInteger errors = new AtomicInteger();

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {
        // Enforce scope in code even if the module is mistakenly enabled for another app.
        if (!TilePolicy.TARGET_PACKAGE.equals(param.packageName)) return;
        log("1.0.0 loaded in " + param.processName);
        installOkHttp(param.classLoader);
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam hook) {
                installOkHttp(((Context) hook.args[0]).getClassLoader());
            }
        });
        installUrlConnection();
    }

    private synchronized void installOkHttp(ClassLoader loader) {
        try {
            Class<?> client = Class.forName("okhttp3.OkHttpClient", false, loader);
            if (installed.contains(client)) return;
            Class<?> request = Class.forName("okhttp3.Request", false, loader);
            Class<?> builder = Class.forName("okhttp3.Request$Builder", false, loader);
            RequestRewriter rewriter = new RequestRewriter(request, builder);
            XposedHelpers.findAndHookMethod(client, "newCall", request, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam hook) {
                    try {
                        Object original = hook.args[0];
                        Object replacement = rewriter.rewrite(original);
                        if (replacement != original) {
                            hook.args[0] = replacement;
                            recordPatch("OkHttp");
                        }
                    } catch (Throwable error) {
                        // Retain the original request and the app's original error behavior.
                        recordError("request", error);
                    }
                }
            });
            installed.add(client);
            log("OkHttp newCall hook installed");
            installResponseLog(loader, rewriter);
        } catch (ClassNotFoundException ignored) {
            // Some protected apps expose their real loader only after Application.attach.
        } catch (Throwable error) {
            recordError("install", error);
        }
    }

    private void installResponseLog(ClassLoader loader, RequestRewriter rewriter) {
        try {
            Class<?> call = Class.forName("okhttp3.internal.connection.RealCall", false, loader);
            Class<?> response = Class.forName("okhttp3.Response", false, loader);
            Method requestMethod = response.getMethod("request");
            Method code = response.getMethod("code");
            Method header = response.getMethod("header", String.class);
            for (Method method : call.getDeclaredMethods()) {
                if (!method.getName().startsWith("getResponseWithInterceptorChain") || method.getParameterTypes().length != 0) continue;
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam hook) {
                        if (hook.hasThrowable() || hook.getResult() == null) return;
                        try {
                            Object result = hook.getResult();
                            if (!rewriter.matches(requestMethod.invoke(result))) return;
                            int count = responses.incrementAndGet();
                            if (count <= 5 || count % 100 == 0) {
                                log("Windy response #" + count + ": HTTP " + code.invoke(result)
                                        + " " + header.invoke(result, "Content-Type"));
                            }
                        } catch (Throwable error) {
                            recordError("response log", error);
                        }
                    }
                });
            }
        } catch (Throwable error) {
            recordError("optional response log", error);
        }
    }

    private void installUrlConnection() {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (param.hasThrowable() || !TilePolicy.matches(param.thisObject.toString())
                        || !(param.getResult() instanceof HttpURLConnection)) return;
                try {
                    ((HttpURLConnection) param.getResult()).setRequestProperty("Referer", TilePolicy.REFERER);
                    recordPatch("HttpURLConnection");
                } catch (Throwable error) {
                    recordError("URLConnection", error);
                }
            }
        };
        XposedBridge.hookAllMethods(URL.class, "openConnection", hook);
    }

    private void recordPatch(String transport) {
        int count = patched.incrementAndGet();
        if (count <= 5 || count % 100 == 0) log("Patched Windy request #" + count + " via " + transport);
    }

    private void recordError(String stage, Throwable error) {
        if (errors.incrementAndGet() <= 5) log(stage + ": " + error.getClass().getSimpleName());
    }

    private static void log(String text) { XposedBridge.log(TAG + text); }
}
