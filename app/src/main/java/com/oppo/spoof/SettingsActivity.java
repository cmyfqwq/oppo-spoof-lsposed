package com.oppo.spoof;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends Activity {

    private LinearLayout cardContainer;
    private SharedPreferences prefs;

    // 新版顶栏/预设卡片视图引用
    private TextView tvPresetLabel;
    private TextView tvPresetBrand;
    private TextView tvPresetModel;
    private TextView tvPresetAndroid;
    private TextView tvEnabledCount;

    // 缓存的开关引用，用于统计
    private final List<Switch> switchList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 沉浸式状态栏适配（让内容不被状态栏遮挡）
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));

        prefs = getSharedPreferences("spoof_config", MODE_PRIVATE);
        cardContainer = findViewById(R.id.card_container);

        // 新版视图
        tvPresetLabel  = findViewById(R.id.tv_preset_label);
        tvPresetBrand  = findViewById(R.id.tv_preset_brand);
        tvPresetModel  = findViewById(R.id.tv_preset_model);
        tvPresetAndroid = findViewById(R.id.tv_preset_android);
        tvEnabledCount = findViewById(R.id.tv_enabled_count);

        // 预设选择按钮
        findViewById(R.id.btn_preset).setOnClickListener(v -> showPresetDialog());

        updatePresetSummary();
        rebuildCards();
    }

    // ─── 重建所有属性卡片 ───
    private void rebuildCards() {
        cardContainer.removeAllViews();
        switchList.clear();

        String[] presetVals = getCurrentPresetVals();
        for (int i = 0; i < PresetData.PROPS.length; i++) {
            PresetData.PropDef prop = PresetData.PROPS[i];
            boolean isLast = (i == PresetData.PROPS.length - 1);
            cardContainer.addView(buildCard(prop, presetVals[prop.presetIdx], isLast));
        }

        updateEnabledCount();
    }

    // ─── 获取当前预设值 ───
    private String[] getCurrentPresetVals() {
        String presetKey = prefs.getString("preset", "oneplus_ace6t");
        return PresetData.getPreset(presetKey);
    }

    // ─── 构建单张属性卡片（Material 3 风格） ───
    @SuppressWarnings("deprecation")
    private View buildCard(final PresetData.PropDef prop, String defaultVal, boolean isLast) {
        // ── 外层卡片 ──
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, isLast ? 0 : dp(10));
        card.setLayoutParams(cardParams);
        card.setBackgroundResource(R.drawable.card_bg);
        card.setPadding(dp(16), dp(14), dp(14), dp(14));

        // ── 第一行：图标 + 标签 + 开关 ──
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 图标 — 放在一个 36dp 圆形浅色底上
        LinearLayout iconWrap = new LinearLayout(this);
        iconWrap.setGravity(Gravity.CENTER);
        iconWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
        iconWrap.setBackgroundColor(getResources().getColor(R.color.primary_container));

        TextView icon = new TextView(this);
        icon.setText(prop.emoji);
        icon.setTextSize(17f);
        icon.setGravity(Gravity.CENTER);
        iconWrap.addView(icon);
        topRow.addView(iconWrap);

        // 间距
        View gap1 = new View(this);
        gap1.setLayoutParams(new LinearLayout.LayoutParams(dp(12), 0));
        topRow.addView(gap1);

        // 标签
        TextView label = new TextView(this);
        label.setText(prop.label);
        label.setTextSize(15f);
        label.setTextColor(getResources().getColor(R.color.text_primary));
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        topRow.addView(label);

        // 占位
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
        topRow.addView(spacer);

        // Material 风格开关
        Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean(prop.enableKey(), false));
        sw.setOnCheckedChangeListener((v, checked) -> {
            prefs.edit().putBoolean(prop.enableKey(), checked).apply();
            updateEnabledCount();
        });
        switchList.add(sw);
        topRow.addView(sw);

        card.addView(topRow);

        // ── 第二行：属性值芯片 ──
        String savedVal = prefs.getString(prop.valueKey(), null);
        String displayVal = (savedVal != null) ? savedVal : defaultVal;

        LinearLayout valueRow = new LinearLayout(this);
        valueRow.setOrientation(LinearLayout.HORIZONTAL);
        valueRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams valRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        valRowParams.setMargins(dp(48), dp(8), 0, 0);
        valueRow.setLayoutParams(valRowParams);

        // 值芯片 — 浅紫蓝底色圆角标签
        TextView valueText = new TextView(this);
        valueText.setText(displayVal);
        valueText.setTextSize(12f);
        valueText.setTextColor(getResources().getColor(R.color.on_primary_container));
        valueText.setBackgroundResource(R.drawable.chip_value_bg);
        valueText.setMaxLines(2);
        valueText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        valueText.setPadding(dp(10), dp(4), dp(10), dp(5));
        valueRow.addView(valueText);

        // 自定义标记（如果用户手动改过值）
        if (savedVal != null) {
            TextView editedMark = new TextView(this);
            editedMark.setText(" ✎");
            editedMark.setTextSize(11f);
            editedMark.setTextColor(getResources().getColor(R.color.primary));
            valueRow.addView(editedMark);
        }

        card.addView(valueRow);

        // ── 长按编辑 ──
        valueRow.setTag(new Object[]{prop, valueText});
        valueRow.setOnLongClickListener(v -> {
            showEditDialog(prop, valueText);
            return true;
        });

        return card;
    }

    // ─── 编辑对话框 ───
    private void showEditDialog(PresetData.PropDef prop, TextView valueText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 标题
        TextView titleView = new TextView(this);
        titleView.setText("修改 " + prop.label);
        titleView.setTextSize(18f);
        titleView.setTextColor(getResources().getColor(R.color.text_primary));
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleView.setPadding(dp(20), dp(20), dp(20), dp(8));
        builder.setCustomTitle(titleView);

        // 输入框
        final EditText input = new EditText(this);
        String current = prefs.getString(prop.valueKey(), "");
        if (current.isEmpty()) {
            current = getCurrentPresetVals()[prop.presetIdx];
        }
        input.setText(current);
        input.setSelectAllOnFocus(true);
        input.setTextSize(15f);
        input.setTextColor(getResources().getColor(R.color.text_primary));
        input.setBackgroundResource(R.drawable.chip_value_bg);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(dp(20), dp(8), dp(20), dp(16));
        wrap.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        builder.setView(wrap);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String val = input.getText().toString().trim();
            if (!val.isEmpty()) {
                prefs.edit().putString(prop.valueKey(), val).apply();
                // 重建卡片以刷新视觉
                rebuildCards();
                Toast.makeText(this, prop.label + " 已更新", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // ─── 预设选择对话框 ───
    private void showPresetDialog() {
        String[] keys = PresetData.PRESETS.keySet().toArray(new String[0]);
        List<String> labelList = new ArrayList<>();
        String currentKey = prefs.getString("preset", "oneplus_ace6t");

        for (String k : keys) {
            String lbl = PresetData.LABELS.get(k);
            String[] vals = PresetData.getPreset(k);
            String check = k.equals(currentKey) ? "  ✓" : "";
            labelList.add((lbl != null ? lbl : k) + "  —  " + vals[PresetData.IDX_MODEL] + check);
        }
        String[] labels = labelList.toArray(new String[0]);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("选择预设机型")
            .setItems(labels, (d, which) -> {
                String presetKey = keys[which];
                prefs.edit().putString("preset", presetKey).apply();

                // 清空所有自定义值
                SharedPreferences.Editor ed = prefs.edit();
                for (PresetData.PropDef p : PresetData.PROPS) {
                    ed.remove(p.valueKey());
                }
                ed.apply();

                updatePresetSummary();
                rebuildCards();
                Toast.makeText(this, "已切换为: " + PresetData.LABELS.get(presetKey),
                        Toast.LENGTH_SHORT).show();
            })
            .create();

        dialog.show();

        // 给对话框按钮染色
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
    }

    // ─── 更新预设摘要区 ───
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

    // ─── 更新启用计数 ───
    private void updateEnabledCount() {
        int count = 0;
        for (Switch sw : switchList) {
            if (sw.isChecked()) count++;
        }
        tvEnabledCount.setText(count + "/10 已启用");
    }

    // ─── dp 转换 ───
    @SuppressWarnings("deprecation")
    private int dp(int px) {
        return (int) (px * getResources().getDisplayMetrics().density);
    }
}
