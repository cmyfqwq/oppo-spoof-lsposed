package com.oppo.spoof;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.util.HashMap;
import java.util.Map;

public class MainHook implements IXposedHookLoadPackage {

    private static final String PKG = "com.cmyf.oppo.spoof";
    private static final String PREFS_NAME = "spoof_config";

    private XSharedPreferences prefs;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        prefs = new XSharedPreferences(PKG, PREFS_NAME);
        prefs.makeWorldReadable();
        prefs.reload();

        String presetKey = prefs.getString("preset", "oneplus_ace6t");
        int presetIdx = PresetData.getPresetIndex(presetKey);
        String[] presetVals = PresetData.getPreset(presetKey);

        XposedBridge.log("[OPPO-Spoof] Hook -> " + lpparam.packageName
                + " | preset=" + presetKey + " (" + presetVals[PresetData.IDX_BRAND]
                + " " + presetVals[PresetData.IDX_MODEL] + ")");

        hookSystemProperties(lpparam, presetVals);
    }

    // ─── Hook SystemProperties ───
    private void hookSystemProperties(XC_LoadPackage.LoadPackageParam lpparam, String[] presetVals) {
        try {
            Class<?> sp = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader);

            // get(String)
            XposedHelpers.findAndHookMethod(sp, "get", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    handleProp((String) param.args[0], param, presetVals);
                }
            });

            // get(String, String)
            XposedHelpers.findAndHookMethod(sp, "get", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    handleProp((String) param.args[0], param, presetVals);
                }
            });

            // getInt(String, int)
            XposedHelpers.findAndHookMethod(sp, "getInt", String.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String key = (String) param.args[0];
                    if ("ro.build.version.sdk".equals(key)) {
                        if (prefs.getBoolean("enable_sdk", false)) {
                            String cv = prefs.getString("value_sdk", null);
                            param.setResult(cv != null ? Integer.parseInt(cv) : Integer.parseInt(presetVals[PresetData.IDX_SDK]));
                        }
                    }
                }
            });

            XposedBridge.log("[OPPO-Spoof] SystemProperties hook OK -> " + lpparam.packageName);
        } catch (Throwable e) {
            XposedBridge.log("[OPPO-Spoof] SystemProperties hook FAIL: " + e.getMessage());
        }
    }

    // ─── 属性欺骗逻辑 ───
    private void handleProp(String key, XC_MethodHook.MethodHookParam param, String[] presetVals) {
        if (key == null) return;

        // 建立系统属性 key → PropDef 的映射
        PropMapping mapping = PROP_KEY_MAP.get(key);
        if (mapping == null) return;

        // 检查是否启用
        if (!prefs.getBoolean(mapping.prop.enableKey(), false)) return;

        // 优先取自定义值，否则取预设值
        String custom = prefs.getString(mapping.prop.valueKey(), null);
        String result = (custom != null && !custom.isEmpty())
                ? custom
                : presetVals[mapping.prop.presetIdx];

        param.setResult(result);
    }

    // ─── 系统属性 Key → PropDef 映射 ───
    private static class PropMapping {
        final PresetData.PropDef prop;
        PropMapping(PresetData.PropDef prop) { this.prop = prop; }
    }

    private static final Map<String, PropMapping> PROP_KEY_MAP = new HashMap<>();
    static {
        // brand
        PROP_KEY_MAP.put("ro.product.brand", new PropMapping(PresetData.PROPS[PresetData.IDX_BRAND]));
        // manufacturer
        PROP_KEY_MAP.put("ro.product.manufacturer", new PropMapping(PresetData.PROPS[PresetData.IDX_MANUFACTURER]));
        // model
        PROP_KEY_MAP.put("ro.product.model", new PropMapping(PresetData.PROPS[PresetData.IDX_MODEL]));
        // device
        PROP_KEY_MAP.put("ro.product.device", new PropMapping(PresetData.PROPS[PresetData.IDX_DEVICE]));
        // product（多个 key 映射到同一属性）
        PROP_KEY_MAP.put("ro.product.name", new PropMapping(PresetData.PROPS[PresetData.IDX_PRODUCT]));
        PROP_KEY_MAP.put("ro.product.board", new PropMapping(PresetData.PROPS[PresetData.IDX_PRODUCT]));
        PROP_KEY_MAP.put("ro.build.product", new PropMapping(PresetData.PROPS[PresetData.IDX_PRODUCT]));
        // hardware
        PROP_KEY_MAP.put("ro.hardware", new PropMapping(PresetData.PROPS[PresetData.IDX_HARDWARE]));
        // fingerprint
        PROP_KEY_MAP.put("ro.build.fingerprint", new PropMapping(PresetData.PROPS[PresetData.IDX_FINGERPRINT]));
        // release
        PROP_KEY_MAP.put("ro.build.version.release", new PropMapping(PresetData.PROPS[PresetData.IDX_RELEASE]));
        // sdk
        PROP_KEY_MAP.put("ro.build.version.sdk", new PropMapping(PresetData.PROPS[PresetData.IDX_SDK]));
        // display
        PROP_KEY_MAP.put("ro.build.display.id", new PropMapping(PresetData.PROPS[PresetData.IDX_DISPLAY]));
    }
}
