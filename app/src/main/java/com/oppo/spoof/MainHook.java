package com.oppo.spoof;

import android.os.Build;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.Field;

public class MainHook implements IXposedHookLoadPackage {

    // 伪装目标设备信息（一加 Ace 6T）
    private static final String FAKE_MANUFACTURER = "OnePlus";
    private static final String FAKE_BRAND        = "OnePlus";
    private static final String FAKE_MODEL        = "ACE6T";
    private static final String FAKE_DEVICE       = "ACE6T";
    private static final String FAKE_PRODUCT      = "ACE6T";
    private static final String FAKE_HARDWARE     = "qcom";
    private static final String FAKE_FINGERPRINT  = "OnePlus/ACE6T/ACE6T:15/AP3A.240905.015.A1/01092349:user/release-keys";
    private static final String FAKE_DISPLAY      = "AP3A.240905.015.A1";
    private static final int    FAKE_SDK_INT      = 35;
    private static final String FAKE_RELEASE      = "15";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        // LSPosed 勾选了哪个 App 就伪装哪个 App，无需包名过滤
        XposedBridge.log("[OPPO-Spoof] 伪装: " + lpparam.packageName);

        // ─── Hook android.os.Build 静态字段 ───
        hookBuildFields();

        // ─── Hook SystemProperties ───
        hookSystemProperties(lpparam);
    }

    private void hookBuildFields() {
        try {
            Class<?> buildClass = Build.class;
            forceSetStatic(buildClass, "MANUFACTURER", FAKE_MANUFACTURER);
            forceSetStatic(buildClass, "BRAND",        FAKE_BRAND);
            forceSetStatic(buildClass, "MODEL",        FAKE_MODEL);
            forceSetStatic(buildClass, "DEVICE",       FAKE_DEVICE);
            forceSetStatic(buildClass, "PRODUCT",      FAKE_PRODUCT);
            forceSetStatic(buildClass, "HARDWARE",     FAKE_HARDWARE);
            forceSetStatic(buildClass, "FINGERPRINT",  FAKE_FINGERPRINT);
            forceSetStatic(buildClass, "DISPLAY",      FAKE_DISPLAY);

            Class<?> versionClass = Build.VERSION.class;
            forceSetStatic(versionClass, "RELEASE",    FAKE_RELEASE);
            forceSetStatic(versionClass, "SDK",        String.valueOf(FAKE_SDK_INT));
            forceSetStaticInt(versionClass, "SDK_INT", FAKE_SDK_INT);
        } catch (Exception e) {
            XposedBridge.log("[OPPO-Spoof] Build hook 失败: " + e.getMessage());
        }
    }

    private void hookSystemProperties(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sp = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader);

            // get(String)
            XposedHelpers.findAndHookMethod(sp, "get", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = (String) param.args[0];
                    switch (key) {
                        case "ro.product.manufacturer":   param.setResult(FAKE_MANUFACTURER); break;
                        case "ro.product.brand":          param.setResult(FAKE_BRAND); break;
                        case "ro.product.model":          param.setResult(FAKE_MODEL); break;
                        case "ro.product.device":         param.setResult(FAKE_DEVICE); break;
                        case "ro.build.version.sdk":      param.setResult(String.valueOf(FAKE_SDK_INT)); break;
                        case "ro.build.version.release":  param.setResult(FAKE_RELEASE); break;
                        case "ro.build.fingerprint":      param.setResult(FAKE_FINGERPRINT); break;
                    }
                }
            });

            // get(String, String)
            XposedHelpers.findAndHookMethod(sp, "get", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = (String) param.args[0];
                    switch (key) {
                        case "ro.product.manufacturer":   param.setResult(FAKE_MANUFACTURER); break;
                        case "ro.product.brand":          param.setResult(FAKE_BRAND); break;
                        case "ro.product.model":          param.setResult(FAKE_MODEL); break;
                        case "ro.product.device":         param.setResult(FAKE_DEVICE); break;
                        case "ro.build.version.sdk":      param.setResult(String.valueOf(FAKE_SDK_INT)); break;
                        case "ro.build.version.release":  param.setResult(FAKE_RELEASE); break;
                        case "ro.build.fingerprint":      param.setResult(FAKE_FINGERPRINT); break;
                    }
                }
            });

            // getInt(String, int)
            XposedHelpers.findAndHookMethod(sp, "getInt", String.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if ("ro.build.version.sdk".equals(param.args[0])) {
                        param.setResult(FAKE_SDK_INT);
                    }
                }
            });

            XposedBridge.log("[OPPO-Spoof] SystemProperties hook OK");
        } catch (Exception e) {
            XposedBridge.log("[OPPO-Spoof] SystemProperties hook 失败: " + e.getMessage());
        }
    }

    // ─── 反射工具 ───
    private void forceSetStatic(Class<?> clazz, String fieldName, Object value) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            // 去掉 final 修饰符（在较新 Android 上可能不需要但也无害）
            try {
                Field modifiers = Field.class.getDeclaredField("accessFlags");
                modifiers.setAccessible(true);
                modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            } catch (Exception ignored) {}
            f.set(null, value);
        } catch (Exception ignored) {}
    }

    private void forceSetStaticInt(Class<?> clazz, String fieldName, int value) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            try {
                Field modifiers = Field.class.getDeclaredField("accessFlags");
                modifiers.setAccessible(true);
                modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            } catch (Exception ignored) {}
            f.setInt(null, value);
        } catch (Exception ignored) {}
    }
}
