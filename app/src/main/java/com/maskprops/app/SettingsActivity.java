package com.maskprops.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import java.util.*;

public class SettingsActivity extends Activity {

    private LinearLayout cardContainer;
    private SharedPreferences prefs;
    private TextView tvPresetLabel, tvPresetBrand, tvPresetModel, tvPresetAndroid;
    private final List<Switch> switchList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));

        prefs = getSharedPreferences("maskprops_config", MODE_PRIVATE);
        cardContainer = findViewById(R.id.card_container);
        tvPresetLabel  = findViewById(R.id.tv_preset_label);
        tvPresetBrand  = findViewById(R.id.tv_preset_brand);
        tvPresetModel  = findViewById(R.id.tv_preset_model);
        tvPresetAndroid = findViewById(R.id.tv_preset_android);

        findViewById(R.id.btn_preset).setOnClickListener(v -> showPresetDialog());
        findViewById(R.id.btn_help).setOnClickListener(v -> showHelpDialog());
        
        // 显示使用帮助（首次启动）
        checkFirstLaunch();

        updatePresetSummary();
        rebuildCards();
    }
    
    /** 检查是否首次启动，显示引导 */
    private void checkFirstLaunch() {
        boolean isFirst = prefs.getBoolean("first_launch", true);
        if (isFirst) {
            showHelpDialog();
            prefs.edit().putBoolean("first_launch", false).apply();
        }
    }
    
    /** 显示使用帮助对话框 */
    private void showHelpDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(16), dp(24), dp(8));
        
        // 标题
        TextView title = new TextView(this);
        title.setText("🎭 欢迎使用 MaskProps");
        title.setTextSize(18f);
        title.setTextColor(getResources().getColor(R.color.text_primary));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);
        
        // 说明文字
        String helpText = "\n📱 功能说明\n" +
                "MaskProps 可以伪装你的设备信息，让应用认为你在使用其他设备。\n" +
                "\n🚀 使用步骤\n" +
                "1. 选择一个预设机型（如「一加 13」）\n" +
                "2. 勾选要伪装的设备属性\n" +
                "3. 在 LSPosed Manager 中勾选本模块\n" +
                "4. 重启设备生效\n" +
                "\n💡 小贴士\n" +
                "• 点击属性卡片下方的值可以自定义修改\n" +
                "• 长按「切换机型」可以快速重置所有属性\n" +
                "• 建议使用预设值，手动修改可能导致指纹不匹配";
        
        TextView help = new TextView(this);
        help.setText(helpText);
        help.setTextSize(13f);
        help.setTextColor(getResources().getColor(R.color.text_secondary));
        help.setLineSpacing(dp(4), 1.0f);
        layout.addView(help);
        
        new AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton("开始使用", null)
            .setNeutralButton("查看详细说明", (d, w) -> showDetailedHelp())
            .show();
    }
    
    /** 显示详细帮助 */
    private void showDetailedHelp() {
        // TODO: 可以打开网页或显示更详细的帮助
        Toast.makeText(this, "详细帮助正在开发中", Toast.LENGTH_SHORT).show();
    }

    // ─── 重建属性卡片 ───
    @SuppressWarnings("deprecation")
    private void rebuildCards() {
        cardContainer.removeAllViews();
        switchList.clear();

        String[] presetVals = getCurrentPresetVals();
        for (int i = 0; i < PresetData.PROPS.length; i++) {
            PresetData.PropDef prop = PresetData.PROPS[i];
            cardContainer.addView(buildCard(prop, presetVals[prop.presetIdx],
                    i == PresetData.PROPS.length - 1));
        }
    }

    private String[] getCurrentPresetVals() {
        return PresetData.getPreset(prefs.getString("preset", "oneplus_ace6t"));
    }

    // ─── 构建单张属性卡片 ───
    @SuppressWarnings("deprecation")
    private View buildCard(PresetData.PropDef prop, String defaultVal, boolean isLast) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        card.setBackgroundResource(R.drawable.card_bg);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        if (!isLast) {
            ((LinearLayout.LayoutParams) card.getLayoutParams()).bottomMargin = dp(10);
        }

        // ── 第一行：标签 + 开关 ──
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(this);
        label.setText(prop.label);
        label.setTextSize(15f);
        label.setTextColor(getResources().getColor(R.color.text_primary));
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        topRow.addView(label);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
        topRow.addView(spacer);

        Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean(prop.enableKey(), false));
        sw.setOnCheckedChangeListener((v, checked) -> {
            prefs.edit().putBoolean(prop.enableKey(), checked).apply();
            card.setAlpha(checked ? 1.0f : 0.4f);
        });
        switchList.add(sw);
        topRow.addView(sw);

        card.addView(topRow);

        // ── 第二行：当前值（可点击编辑） ──
        String savedVal = prefs.getString(prop.valueKey(), null);
        String displayVal = (savedVal != null && !savedVal.isEmpty()) ? savedVal : defaultVal;
        boolean isCustom = (savedVal != null && !savedVal.isEmpty());

        LinearLayout valueRow = new LinearLayout(this);
        valueRow.setOrientation(LinearLayout.HORIZONTAL);
        valueRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams vrParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vrParams.topMargin = dp(8);
        valueRow.setLayoutParams(vrParams);

        TextView valueText = new TextView(this);
        valueText.setText(displayVal);
        valueText.setTextSize(13f);
        valueText.setTextColor(isCustom
                ? getResources().getColor(R.color.primary)
                : getResources().getColor(R.color.text_secondary));
        valueText.setBackgroundResource(R.drawable.chip_value_bg);
        valueText.setMaxLines(2);
        valueText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        valueText.setPadding(dp(12), dp(6), dp(12), dp(6));
        valueRow.addView(valueText);

        valueRow.setClickable(true);
        valueRow.setFocusable(true);
        valueRow.setOnClickListener(v -> showEditDialog(prop, valueText));

        card.addView(valueRow);

        // ── 第三行：属性说明 ──
        TextView descText = new TextView(this);
        descText.setText(prop.desc);
        descText.setTextSize(11f);
        descText.setTextColor(getResources().getColor(R.color.text_tertiary));
        descText.setMaxLines(2);
        descText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = dp(6);
        descText.setLayoutParams(descParams);
        card.addView(descText);

        // 初始透明度
        card.setAlpha(sw.isChecked() ? 1.0f : 0.4f);

        return card;
    }

    // ─── 编辑对话框 ───
    @SuppressWarnings("deprecation")
    private void showEditDialog(PresetData.PropDef prop, TextView valueText) {
        String current = prefs.getString(prop.valueKey(), "");
        if (current.isEmpty()) {
            current = getCurrentPresetVals()[prop.presetIdx];
        }
        String savedVal = prefs.getString(prop.valueKey(), null);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(24), dp(8), dp(24), dp(4));

        final EditText input = new EditText(this);
        input.setText(current);
        input.setSelectAllOnFocus(true);
        input.setTextSize(15f);
        input.setTextColor(getResources().getColor(R.color.text_primary));
        input.setBackgroundColor(getResources().getColor(R.color.surface_variant));
        input.setPadding(dp(14), dp(14), dp(14), dp(14));
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        wrap.addView(input);

        if (savedVal != null && !savedVal.isEmpty()) {
            String presetVal = getCurrentPresetVals()[prop.presetIdx];
            TextView hint = new TextView(this);
            hint.setText("预设值: " + presetVal);
            hint.setTextSize(11f);
            hint.setTextColor(getResources().getColor(R.color.text_tertiary));
            hint.setPadding(dp(4), dp(8), 0, 0);
            wrap.addView(hint);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(prop.label)
            .setView(wrap)
            .setPositiveButton("确定", (dialog, which) -> {
                String val = input.getText().toString().trim();
                if (!val.isEmpty()) {
                    prefs.edit().putString(prop.valueKey(), val).apply();
                    rebuildCards();
                }
            })
            .setNegativeButton("取消", null);

        if (savedVal != null && !savedVal.isEmpty()) {
            builder.setNeutralButton("重置", (dialog, which) -> {
                prefs.edit().remove(prop.valueKey()).apply();
                rebuildCards();
            });
        }

        builder.show();
    }

    // ─── 预设选择对话框 ───
    @SuppressWarnings("deprecation")
    private void showPresetDialog() {
        String[] keys = PresetData.PRESETS.keySet().toArray(new String[0]);
        String currentKey = prefs.getString("preset", "oneplus_ace6t");

        LinearLayout listView = new LinearLayout(this);
        listView.setOrientation(LinearLayout.VERTICAL);
        listView.setPadding(dp(8), dp(4), dp(8), dp(4));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("选择预设机型")
            .setView(listView)
            .create();

        for (int i = 0; i < keys.length; i++) {
            String k = keys[i];
            String[] vals = PresetData.getPreset(k);
            String label = PresetData.LABELS.get(k);
            boolean isCurrent = k.equals(currentKey);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(14), dp(16), dp(14));
            row.setClickable(true);
            row.setFocusable(true);

            LinearLayout mid = new LinearLayout(this);
            mid.setOrientation(LinearLayout.VERTICAL);
            mid.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView name = new TextView(this);
            name.setText(label);
            name.setTextSize(15f);
            name.setTextColor(getResources().getColor(
                    isCurrent ? R.color.primary : R.color.text_primary));
            name.setTypeface(Typeface.DEFAULT, isCurrent ? Typeface.BOLD : Typeface.NORMAL);
            mid.addView(name);

            TextView model = new TextView(this);
            model.setText(vals[PresetData.IDX_BRAND] + " " + vals[PresetData.IDX_MODEL]
                    + "  ·  Android " + vals[PresetData.IDX_RELEASE]);
            model.setTextSize(12f);
            model.setTextColor(getResources().getColor(R.color.text_secondary));
            mid.addView(model);

            row.addView(mid);

            if (isCurrent) {
                TextView check = new TextView(this);
                check.setText(" ✓");
                check.setTextSize(16f);
                check.setTextColor(getResources().getColor(R.color.primary));
                row.addView(check);
            }

            final String presetKey = k;
            row.setOnClickListener(v -> {
                dialog.dismiss();
                applyPreset(presetKey);
            });

            listView.addView(row);

            if (i < keys.length - 1) {
                View div = new View(this);
                div.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
                div.setBackgroundColor(getResources().getColor(R.color.border));
                listView.addView(div);
            }
        }

        dialog.show();
    }

    private void applyPreset(String presetKey) {
        prefs.edit().putString("preset", presetKey).apply();
        SharedPreferences.Editor ed = prefs.edit();
        for (PresetData.PropDef p : PresetData.PROPS) {
            ed.remove(p.valueKey());
        }
        ed.apply();
        updatePresetSummary();
        rebuildCards();
    }

    private void updatePresetSummary() {
        String presetKey = prefs.getString("preset", "oneplus_ace6t");
        String[] vals = PresetData.getPreset(presetKey);
        String label = PresetData.LABELS.get(presetKey);

        tvPresetLabel.setText(label != null ? label : presetKey);
        tvPresetBrand.setText(vals[PresetData.IDX_BRAND]);
        tvPresetModel.setText(vals[PresetData.IDX_MODEL]);
        tvPresetAndroid.setText("Android " + vals[PresetData.IDX_RELEASE]
                + "  (SDK " + vals[PresetData.IDX_SDK] + ")");
    }

    @SuppressWarnings("deprecation")
    private int dp(int px) {
        return (int) (px * getResources().getDisplayMetrics().density);
    }
}
