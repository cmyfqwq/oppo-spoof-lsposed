package com.maskprops.app;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 共享预设数据 —— MainHook 和 SettingsActivity 共用，避免重复维护。
 * <p>
 * 每个预设包含 10 个字段，通过索引常量访问：
 * <pre>
 *   IDX_BRAND, IDX_MANUFACTURER, IDX_MODEL, IDX_DEVICE, IDX_PRODUCT,
 *   IDX_HARDWARE, IDX_FINGERPRINT, IDX_RELEASE, IDX_SDK, IDX_DISPLAY
 * </pre>
 */
public final class PresetData {

    private PresetData() {}

    // ─── 索引常量 ───
    public static final int IDX_BRAND        = 0;
    public static final int IDX_MANUFACTURER = 1;
    public static final int IDX_MODEL        = 2;
    public static final int IDX_DEVICE       = 3;
    public static final int IDX_PRODUCT      = 4;
    public static final int IDX_HARDWARE     = 5;
    public static final int IDX_FINGERPRINT  = 6;
    public static final int IDX_RELEASE      = 7;
    public static final int IDX_SDK          = 8;
    public static final int IDX_DISPLAY      = 9;

    public static final int FIELD_COUNT = 10;

    // ─── 预设机型库 ───
    public static final Map<String, String[]> PRESETS = new LinkedHashMap<>();
    static {
        PRESETS.put("oneplus_ace6t", arr(
            "OnePlus", "OnePlus",
            "ACE6T", "ACE6T", "ACE6T",
            "qcom",
            "OnePlus/ACE6T/ACE6T:15/A3.240905.015.A1/01092349:user/release-keys",
            "15", "35",
            "A3.240905.015.A1"
        ));
        PRESETS.put("oneplus_13", arr(
            "OnePlus", "OnePlus",
            "PJZ110", "PJZ110", "PJZ110",
            "qcom",
            "OnePlus/PJZ110/PJZ110:15/AQ3A.240912.001/01092349:user/release-keys",
            "15", "35",
            "AQ3A.240912.001"
        ));
        PRESETS.put("oppo_find_x8", arr(
            "OPPO", "OPPO",
            "PKC110", "PKC110", "PKC110",
            "mt6897",
            "OPPO/PKC110/PKC110:15/AP3A.240905.015.A1/01092349:user/release-keys",
            "15", "35",
            "AP3A.240905.015.A1"
        ));
        PRESETS.put("xiaomi_14", arr(
            "Xiaomi", "Xiaomi",
            "23127PN0CC", "houji", "houji",
            "qcom",
            "Xiaomi/houji/houji:14/UKQ1.230804.001/V816.0.8.0.UNCCNXM:user/release-keys",
            "14", "34",
            "UKQ1.230804.001"
        ));
        PRESETS.put("huawei_mate60pro", arr(
            "HUAWEI", "HUAWEI",
            "ALN-AL00", "ALN-AL00", "ALN-AL00",
            "kirin9000s",
            "HUAWEI/ALN-AL00/HWALN:12/HUAWEIALN-AL00/103.0.0.168:user/release-keys",
            "12", "31",
            "103.0.0.168"
        ));
        PRESETS.put("samsung_s24ultra", arr(
            "samsung", "samsung",
            "SM-S9280", "q5q", "q5qzcx",
            "qcom",
            "samsung/q5qzcx/q5q:14/UP1A.231005.007/S9280ZCU1AXB7:user/release-keys",
            "14", "34",
            "UP1A.231005.007"
        ));
        PRESETS.put("vivo_x100pro", arr(
            "vivo", "vivo",
            "V2309A", "V2309A", "V2309A",
            "mt6989",
            "vivo/V2309A/V2309A:14/UP1A.231005.007/compiler11232138:user/release-keys",
            "14", "34",
            "UP1A.231005.007"
        ));
    }

    // ─── 显示名称 ───
    public static final Map<String, String> LABELS = new LinkedHashMap<>();
    static {
        LABELS.put("oneplus_ace6t",    "一加 Ace 6T");
        LABELS.put("oneplus_13",       "一加 13");
        LABELS.put("oppo_find_x8",     "OPPO Find X8");
        LABELS.put("xiaomi_14",        "小米 14");
        LABELS.put("huawei_mate60pro", "华为 Mate 60 Pro");
        LABELS.put("samsung_s24ultra", "三星 S24 Ultra");
        LABELS.put("vivo_x100pro",     "vivo X100 Pro");
    }

    // ─── 属性定义 ───
    public static class PropDef {
        public final String key;       // SharedPreference key
        public final String label;     // 中文标签
        public final String desc;      // 属性说明
        public final int presetIdx;    // 对应 PRESETS 数组的索引

        public PropDef(String key, String label, String desc, int presetIdx) {
            this.key = key;
            this.label = label;
            this.desc = desc;
            this.presetIdx = presetIdx;
        }

        /** 完整的 SharedPreferences enable key */
        public String enableKey() { return "enable_" + key; }

        /** 完整的 SharedPreferences value key */
        public String valueKey() { return "value_" + key; }
    }

    public static final PropDef[] PROPS = {
        new PropDef("brand",        "品牌",        "设备品牌名称，如 OnePlus、Xiaomi", IDX_BRAND),
        new PropDef("manufacturer", "制造商",       "设备制造商，通常与品牌相同", IDX_MANUFACTURER),
        new PropDef("model",        "型号",         "设备具体型号，如 ACE6T、23127PN0CC", IDX_MODEL),
        new PropDef("device",       "设备代号",      "开发代号，如 ace6t、houji", IDX_DEVICE),
        new PropDef("product",      "产品名",       "产品名称，通常与设备代号相同", IDX_PRODUCT),
        new PropDef("hardware",     "硬件",         "硬件平台标识，如 qcom（高通）、mt6989（联发科）", IDX_HARDWARE),
        new PropDef("fingerprint",  "构建指纹",      "系统唯一标识，包含品牌/型号/版本等信息", IDX_FINGERPRINT),
        new PropDef("release",      "Android 版本",  "Android 系统版本号，如 14、15", IDX_RELEASE),
        new PropDef("sdk",          "SDK 版本",     "Android API 级别，如 34（Android 14）、35（Android 15）", IDX_SDK),
        new PropDef("display",      "显示 ID",      "系统构建显示的版本标识", IDX_DISPLAY),
    };

    // ─── 系统属性 Key 映射 ───
    public static String[] SYSTEM_PROP_KEYS = {
        "ro.product.brand",       // IDX_BRAND
        "ro.product.manufacturer", // IDX_MANUFACTURER
        "ro.product.model",       // IDX_MODEL
        "ro.product.device",      // IDX_DEVICE
        "ro.product.name",        // IDX_PRODUCT  (also: ro.product.board, ro.build.product)
        "ro.hardware",            // IDX_HARDWARE
        "ro.build.fingerprint",   // IDX_FINGERPRINT
        "ro.build.version.release", // IDX_RELEASE
        "ro.build.version.sdk",   // IDX_SDK
        "ro.build.display.id",    // IDX_DISPLAY
    };

    // ─── 辅助方法 ───
    private static String[] arr(String... vs) {
        if (vs.length != FIELD_COUNT) {
            throw new IllegalArgumentException("Preset must have exactly " + FIELD_COUNT + " fields, got " + vs.length);
        }
        return vs;
    }

    /** 根据 key 获取预设值，不存在时 fallback 到 oneplus_ace6t */
    public static String[] getPreset(String key) {
        String[] vals = PRESETS.get(key);
        return vals != null ? vals : PRESETS.get("oneplus_ace6t");
    }

    /** 根据索引获取预设值并 fallback */
    public static String[] getPresetByIndex(int idx) {
        int i = 0;
        for (String[] vals : PRESETS.values()) {
            if (i++ == idx) return vals;
        }
        return PRESETS.get("oneplus_ace6t");
    }

    /** 根据 key 获取预设索引 */
    public static int getPresetIndex(String key) {
        int i = 0;
        for (String k : PRESETS.keySet()) {
            if (k.equals(key)) return i;
            i++;
        }
        return 0;
    }
}
