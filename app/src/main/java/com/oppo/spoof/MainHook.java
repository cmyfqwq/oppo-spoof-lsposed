package com.oppo.spoof;

import android.os.Build;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainHook implements IXposedHookLoadPackage {

    // 伪装目标设备信息（一加 Ace 6T）
    private static final String FAKE_MANUFACTURER = "OnePlus";
    private static final String FAKE_BRAND        = "OnePlus";
    private static final String FAKE_MODEL        = "PJF110";
    private static final String FAKE_DEVICE       = "PJF110";
    private static final String FAKE_PRODUCT      = "PJF110";
    private static final String FAKE_HARDWARE     = "qcom";
    private static final String FAKE_FINGERPRINT  = "OnePlus/PJF110/PJF110:15/AP3A.240905.015.A1/01092349:user/release-keys";
    private static final String FAKE_DISPLAY      = "AP3A.240905.015.A1";
    private static final int    FAKE_SDK_INT     = 35;
    private static final String FAKE_RELEASE      = "15";

    private Set<String> targetPackages = new HashSet<>();
    private List<String> prefixList    = new ArrayList<>();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        // 加载包名配置（只加载一次）
        if (targetPackages.isEmpty() && prefixList.isEmpty()) {
            loadPackages();
        }

        String pkg = lpparam.packageName;

        // 判断是否目标包名
        if (!isTargetPackage(pkg)) return;

        XposedBridge.log("[OPPO-Spoof] 伪装包名: " + pkg);

        // ─── Hook android.os.Build 静态字段 ───
        try {
            Class<?> buildClass = Build.class;

            setStaticField(buildClass, "MANUFACTURER", FAKE_MANUFACTURER);
            setStaticField(buildClass, "BRAND",        FAKE_BRAND);
            setStaticField(buildClass, "MODEL",        FAKE_MODEL);
            setStaticField(buildClass, "DEVICE",       FAKE_DEVICE);
            setStaticField(buildClass, "PRODUCT",      FAKE_PRODUCT);
            setStaticField(buildClass, "HARDWARE",     FAKE_HARDWARE);
            setStaticField(buildClass, "FINGERPRINT",  FAKE_FINGERPRINT);
            setStaticField(buildClass, "DISPLAY",      FAKE_DISPLAY);

            // VERSION 内部类
            Class<?> versionClass = Build.VERSION.class;
            setStaticField(versionClass, "RELEASE",    FAKE_RELEASE);
            setStaticField(versionClass, "SDK",        String.valueOf(FAKE_SDK_INT));
            setStaticFieldInt(versionClass, "SDK_INT", FAKE_SDK_INT);

        } catch (Exception e) {
            XposedBridge.log("[OPPO-Spoof] Hook Build 失败: " + e.getMessage());
        }

        // ─── Hook SystemProperties 的 native 读取（关键！解决 SDK_INT 问题） ───
        hookSystemProperties(lpparam);
    }

    // ─── 加载包名配置 ───
    private void loadPackages() {
        // 按优先级尝试从这些路径读取配置文件
        String[] configPaths = {
            "/sdcard/oppo-spoof/packages.txt",       // 用户可轻松编辑
            "/storage/emulated/0/oppo-spoof/packages.txt",
            "/data/adb/modules/oppo_mask/config/packages.txt",   // 复用 Zygisk 版配置
            "/data/adb/modules/oppo-spoof-lsposed/config/packages.txt",
        };

        for (String path : configPaths) {
            if (tryLoadFromFile(path)) {
                XposedBridge.log("[OPPO-Spoof] 已从文件加载配置: " + path);
                XposedBridge.log("[OPPO-Spoof] 已加载 " + targetPackages.size() + " 个精确包名, "
                        + prefixList.size() + " 个前缀规则");
                return;
            }
        }

        // 所有文件都读不到，使用内置默认列表
        addDefaultPackages();
        XposedBridge.log("[OPPO-Spoof] 未找到配置文件，使用内置默认包名列表");
        XposedBridge.log("[OPPO-Spoof] 已加载 " + targetPackages.size() + " 个精确包名, "
                + prefixList.size() + " 个前缀规则");
    }

    private boolean tryLoadFromFile(String path) {
        File f = new File(path);
        if (!f.exists() || !f.canRead()) return false;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.endsWith(".")) {
                    prefixList.add(line.substring(0, line.length() - 1));
                } else {
                    targetPackages.add(line);
                }
            }
            return !targetPackages.isEmpty() || !prefixList.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private void addDefaultPackages() {
        targetPackages.add("com.heytap.speechassist");
        targetPackages.add("com.heytap.browser");
        targetPackages.add("com.heytap.cloud");
        targetPackages.add("com.heytap.health");
        targetPackages.add("com.coloros.phonemanager");
        targetPackages.add("com.coloros.securitypermission");
        targetPackages.add("com.coloros.gamespace");
        targetPackages.add("com.coloros.filemanager");
        targetPackages.add("com.nearme.gamecenter");
        targetPackages.add("com.nearme.instantly");
        targetPackages.add("com.finshell.wallet");
        targetPackages.add("com.finshell.fin");

        prefixList.add("com.heytap");
        prefixList.add("com.coloros");
        prefixList.add("com.nearme");
        prefixList.add("com.oppo");
        prefixList.add("com.oplus");
    }

    private boolean isTargetPackage(String pkg) {
        if (targetPackages.contains(pkg)) return true;
        for (String prefix : prefixList) {
            if (pkg.startsWith(prefix)) return true;
        }
        return false;
    }

    // ─── 反射设置静态字段（兼容 final 字段） ───
    private void setStaticField(Class<?> clazz, String fieldName, Object value) {
        try {
            Field f = XposedHelpers.findField(clazz, fieldName);
            f.setAccessible(true);
            // 移除 final 修饰符
            XposedHelpers.setStaticObjectField(clazz, fieldName, value);
        } catch (Exception e) {
            // ignore
        }
    }

    private void setStaticFieldInt(Class<?> clazz, String fieldName, int value) {
        try {
            Field f = XposedHelpers.findField(clazz, fieldName);
            f.setAccessible(true);
            XposedHelpers.setStaticIntField(clazz, fieldName, value);
        } catch (Exception e) {
            // ignore
        }
    }

    // ─── Hook SystemProperties（解决 SDK_INT 被 native 读取的问题） ───
    private void hookSystemProperties(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> spClass = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader);

            // hook get(String)
            XposedHelpers.findAndHookMethod(spClass, "get", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = (String) param.args[0];
                    if ("ro.product.manufacturer".equals(key)) {
                        param.setResult(FAKE_MANUFACTURER);
                    } else if ("ro.product.brand".equals(key)) {
                        param.setResult(FAKE_BRAND);
                    } else if ("ro.product.model".equals(key)) {
                        param.setResult(FAKE_MODEL);
                    } else if ("ro.product.device".equals(key)) {
                        param.setResult(FAKE_DEVICE);
                    } else if ("ro.build.version.sdk".equals(key)) {
                        param.setResult(String.valueOf(FAKE_SDK_INT));
                    } else if ("ro.build.version.release".equals(key)) {
                        param.setResult(FAKE_RELEASE);
                    } else if ("ro.build.fingerprint".equals(key)) {
                        param.setResult(FAKE_FINGERPRINT);
                    }
                }
            });

            // hook get(String, String)
            XposedHelpers.findAndHookMethod(spClass, "get", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = (String) param.args[0];
                    if ("ro.product.manufacturer".equals(key)) {
                        param.setResult(FAKE_MANUFACTURER);
                    } else if ("ro.product.brand".equals(key)) {
                        param.setResult(FAKE_BRAND);
                    } else if ("ro.product.model".equals(key)) {
                        param.setResult(FAKE_MODEL);
                    } else if ("ro.product.device".equals(key)) {
                        param.setResult(FAKE_DEVICE);
                    } else if ("ro.build.version.sdk".equals(key)) {
                        param.setResult(String.valueOf(FAKE_SDK_INT));
                    } else if ("ro.build.version.release".equals(key)) {
                        param.setResult(FAKE_RELEASE);
                    } else if ("ro.build.fingerprint".equals(key)) {
                        param.setResult(FAKE_FINGERPRINT);
                    }
                }
            });

            // hook getInt(String, int)
            XposedHelpers.findAndHookMethod(spClass, "getInt", String.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = (String) param.args[0];
                    if ("ro.build.version.sdk".equals(key)) {
                        param.setResult(FAKE_SDK_INT);
                    }
                }
            });

            XposedBridge.log("[OPPO-Spoof] SystemProperties hook 成功");
        } catch (Exception e) {
            XposedBridge.log("[OPPO-Spoof] SystemProperties hook 失败: " + e.getMessage());
        }
    }
}
