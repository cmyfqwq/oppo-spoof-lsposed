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
import java.util.LinkedHashMap;
import java.util.Map;

public class MainHook implements IXposedHookLoadPackage {

    private static final String CONFIG_PATH = "/sdcard/oppo-spoof/config.txt";

    // ─── 预设机型库 ───
    // 每个预设包含完整的设备信息
    private static final Map<String, DeviceProfile> PRESETS = new LinkedHashMap<>();
    static {
        // 一加 Ace 6T（默认）
        PRESETS.put("oneplus_ace6t", new DeviceProfile(
                "OnePlus", "OnePlus", "ACE6T", "ACE6T", "ACE6T", "qcom",
                "OnePlus/ACE6T/ACE6T:15/AP3A.240905.015.A1/01092349:user/release-keys",
                "15", 35, "AP3A.240905.015.A1"
        ));
        // 一加 13
        PRESETS.put("oneplus_13", new DeviceProfile(
                "OnePlus", "OnePlus", "PJZ110", "PJZ110", "PJZ110", "qcom",
                "OnePlus/PJZ110/PJZ110:15/AQ3A.240912.001/01092349:user/release-keys",
                "15", 35, "AQ3A.240912.001"
        ));
        // OPPO Find X8
        PRESETS.put("oppo_find_x8", new DeviceProfile(
                "OPPO", "OPPO", "PKC110", "PKC110", "PKC110", "mt6897",
                "OPPO/PKC110/PKC110:15/AP3A.240905.015.A1/01092349:user/release-keys",
                "15", 35, "AP3A.240905.015.A1"
        ));
        // 小米 14
        PRESETS.put("xiaomi_14", new DeviceProfile(
                "Xiaomi", "Xiaomi", "23127PN0CC", "houji", "houji", "qcom",
                "Xiaomi/houji/houji:14/UKQ1.230804.001/V816.0.8.0.UNCCNXM:user/release-keys",
                "14", 34, "UKQ1.230804.001"
        ));
        // 华为 Mate 60 Pro
        PRESETS.put("huawei_mate60pro", new DeviceProfile(
                "HUAWEI", "HUAWEI", "ALN-AL00", "ALN-AL00", "ALN-AL00", "kirin9000s",
                "HUAWEI/ALN-AL00/HWALN:12/HUAWEIALN-AL00/103.0.0.168:user/release-keys",
                "12", 31, "103.0.0.168"
        ));
        // 三星 S24 Ultra
        PRESETS.put("samsung_s24ultra", new DeviceProfile(
                "samsung", "samsung", "SM-S9280", "q5q", "q5qzcx", "qcom",
                "samsung/q5qzcx/q5q:14/UP1A.231005.007/S9280ZCU1AXB7:user/release-keys",
                "14", 34, "UP1A.231005.007"
        ));
        // vivo X100 Pro
        PRESETS.put("vivo_x100pro", new DeviceProfile(
                "vivo", "vivo", "V2309A", "V2309A", "V2309A", "mt6989",
                "vivo/V2309A/V2309A:14/UP1A.231005.007/compiler11232138:user/release-keys",
                "14", 34, "UP1A.231005.007"
        ));
    }

    // ─── 当前生效的伪装信息 ───
    private static DeviceProfile profile;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        // 延迟加载：第一次 Hook 时才读取配置
        if (profile == null) {
            profile = loadProfile();
        }

        XposedBridge.log("[OPPO-Spoof] 伪装: " + lpparam.packageName +
                " -> " + profile.brand + " " + profile.model);

        hookBuildFields();
        hookSystemProperties(lpparam);
    }

    // ─── 读取配置 / 预设 ───
    private DeviceProfile loadProfile() {
        File configFile = new File(CONFIG_PATH);
        String presetName = null;
        Map<String, String> overrides = new LinkedHashMap<>();

        if (configFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eq = line.indexOf('=');
                    if (eq <= 0) continue;
                    String key = line.substring(0, eq).trim();
                    String val = line.substring(eq + 1).trim();
                    if ("preset".equals(key)) {
                        presetName = val;
                    } else {
                        overrides.put(key, val);
                    }
                }
            } catch (Exception e) {
                XposedBridge.log("[OPPO-Spoof] 读取配置文件失败: " + e.getMessage());
            }
        }

        // 选择预设
        DeviceProfile selected;
        if (presetName != null && PRESETS.containsKey(presetName)) {
            selected = PRESETS.get(presetName).clone();
        } else {
            selected = PRESETS.get("oneplus_ace6t").clone();  // 默认一加 Ace 6T
        }

        // 应用覆盖值（用户可单独修改某个字段）
        for (Map.Entry<String, String> e : overrides.entrySet()) {
            applyOverride(selected, e.getKey(), e.getValue());
        }

        XposedBridge.log("[OPPO-Spoof] 使用预设: " +
                (presetName != null ? presetName : "默认(一加Ace6T)"));
        return selected;
    }

    private void applyOverride(DeviceProfile p, String key, String value) {
        switch (key) {
            case "brand":        p.brand        = value; break;
            case "manufacturer": p.manufacturer = value; break;
            case "model":        p.model        = value; break;
            case "device":       p.device       = value; break;
            case "product":      p.product      = value; break;
            case "hardware":     p.hardware     = value; break;
            case "fingerprint":  p.fingerprint  = value; break;
            case "release":      p.release      = value; break;
            case "display":      p.display      = value; break;
            case "sdk":          try { p.sdk = Integer.parseInt(value); } catch (Exception ignored) {} break;
        }
    }

    // ─── Hook Build 字段 ───
    private void hookBuildFields() {
        try {
            Class<?> bc = Build.class;
            forceSetStatic(bc, "MANUFACTURER", profile.manufacturer);
            forceSetStatic(bc, "BRAND",        profile.brand);
            forceSetStatic(bc, "MODEL",        profile.model);
            forceSetStatic(bc, "DEVICE",       profile.device);
            forceSetStatic(bc, "PRODUCT",      profile.product);
            forceSetStatic(bc, "HARDWARE",     profile.hardware);
            forceSetStatic(bc, "FINGERPRINT",  profile.fingerprint);
            forceSetStatic(bc, "DISPLAY",      profile.display);

            Class<?> vc = Build.VERSION.class;
            forceSetStatic(vc, "RELEASE",    profile.release);
            forceSetStatic(vc, "SDK",        String.valueOf(profile.sdk));
            forceSetStaticInt(vc, "SDK_INT", profile.sdk);
        } catch (Exception e) {
            XposedBridge.log("[OPPO-Spoof] Build hook 失败: " + e.getMessage());
        }
    }

    // ─── Hook SystemProperties ───
    private void hookSystemProperties(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sp = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(sp, "get", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    handleSpGet((String) param.args[0], param);
                }
            });
            XposedHelpers.findAndHookMethod(sp, "get", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    handleSpGet((String) param.args[0], param);
                }
            });
            XposedHelpers.findAndHookMethod(sp, "getInt", String.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if ("ro.build.version.sdk".equals(param.args[0])) {
                        param.setResult(profile.sdk);
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log("[OPPO-Spoof] SystemProperties hook 失败: " + e.getMessage());
        }
    }

    private void handleSpGet(String key, XC_MethodHook.MethodHookParam param) {
        switch (key) {
            case "ro.product.manufacturer":  param.setResult(profile.manufacturer); break;
            case "ro.product.brand":         param.setResult(profile.brand);        break;
            case "ro.product.model":         param.setResult(profile.model);        break;
            case "ro.product.device":        param.setResult(profile.device);       break;
            case "ro.build.version.sdk":     param.setResult(String.valueOf(profile.sdk)); break;
            case "ro.build.version.release": param.setResult(profile.release);      break;
            case "ro.build.fingerprint":     param.setResult(profile.fingerprint);  break;
        }
    }

    // ─── 反射工具 ───
    private void forceSetStatic(Class<?> clazz, String fieldName, Object value) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            try {
                Field af = Field.class.getDeclaredField("accessFlags");
                af.setAccessible(true);
                af.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            } catch (Exception ignored) {}
            f.set(null, value);
        } catch (Exception ignored) {}
    }

    private void forceSetStaticInt(Class<?> clazz, String fieldName, int value) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            try {
                Field af = Field.class.getDeclaredField("accessFlags");
                af.setAccessible(true);
                af.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            } catch (Exception ignored) {}
            f.setInt(null, value);
        } catch (Exception ignored) {}
    }

    // ─── 设备信息数据类 ───
    private static class DeviceProfile implements Cloneable {
        String brand, manufacturer, model, device, product, hardware;
        String fingerprint, release, display;
        int sdk;

        DeviceProfile(String brand, String manufacturer, String model, String device,
                      String product, String hardware, String fingerprint,
                      String release, int sdk, String display) {
            this.brand        = brand;
            this.manufacturer = manufacturer;
            this.model        = model;
            this.device       = device;
            this.product      = product;
            this.hardware     = hardware;
            this.fingerprint  = fingerprint;
            this.release      = release;
            this.sdk          = sdk;
            this.display      = display;
        }

        @Override
        protected DeviceProfile clone() {
            try { return (DeviceProfile) super.clone(); } catch (Exception e) { return null; }
        }
    }
}
