package com.oppo.spoof;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettingsActivity extends Activity {

    private LinearLayout cardContainer;
    private SharedPreferences prefs;
    private TextView tvPresetSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("spoof_config", MODE_PRIVATE);
        cardContainer = findViewById(R.id.card_container);
        tvPresetSummary = findViewById(R.id.tv_preset_summary);

        // 预设选择按钮
        findViewById(R.id.btn_preset).setOnClickListener(v -> showPresetDialog());

        // 更新预设摘要
        updatePresetSummary();

        // 构建所有属性卡片
        rebuildCards();
    }

    // ─── 重建所有属性卡片 ───
    private void rebuildCards() {
        cardContainer.removeAllViews();
        String[] presetVals = getCurrentPresetVals();
        for (PresetData.PropDef prop : PresetData.PROPS) {
            cardContainer.addView(buildCard(prop, presetVals[prop.presetIdx]));
        }
    }

    // ─── 获取当前预设值 ───
    private String[] getCurrentPresetVals() {
        String presetKey = prefs.getString("preset", "oneplus_ace6t");
        return PresetData.getPreset(presetKey);
    }

    // ─── 构建单张属性卡片 ───
    private View buildCard(final PresetData.PropDef prop, String defaultVal) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(cardParams);
        card.setBackgroundResource(R.drawable.card_bg);
        card.setPadding(dp(12), dp(10), dp(8), dp(10));

        // 第一行：图标 + 标签 + 开关
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 图标
        TextView icon = new TextView(this);
        icon.setText(prop.emoji);
        icon.setTextSize(20f);
        icon.setPadding(0, 0, dp(10), 0);
        topRow.addView(icon);

        // 标签
        TextView label = new TextView(this);
        label.setText(prop.label);
        label.setTextSize(15f);
        label.setTextColor(Color.parseColor("#212121"));
        label.setTypeface(null, Typeface.BOLD);
        topRow.addView(label);

        // 占位
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
        topRow.addView(spacer);

        // 开关
        Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean(prop.enableKey(), false));
        sw.setOnCheckedChangeListener((v, checked) ->
                prefs.edit().putBoolean(prop.enableKey(), checked).apply());
        topRow.addView(sw);

        card.addView(topRow);

        // 第二行：当前值
        String savedVal = prefs.getString(prop.valueKey(), null);
        String displayVal = (savedVal != null) ? savedVal : defaultVal;
        TextView valueText = new TextView(this);
        valueText.setText(displayVal);
        valueText.setTextSize(13f);
        valueText.setTextColor(Color.parseColor("#757575"));
        valueText.setPadding(dp(30), dp(2), 0, 0);
        card.addView(valueText);

        // 长按编辑
        valueText.setTag(prop);
        valueText.setOnLongClickListener(v -> {
            showEditDialog(prop, valueText);
            return true;
        });

        return card;
    }

    // ─── 编辑对话框 ───
    private void showEditDialog(PresetData.PropDef prop, TextView valueText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("修改 " + prop.label);

        final EditText input = new EditText(this);
        String current = prefs.getString(prop.valueKey(), "");
        if (current.isEmpty()) {
            // 取预设默认值
            current = getCurrentPresetVals()[prop.presetIdx];
        }
        input.setText(current);
        input.setSelectAllOnFocus(true);
        builder.setView(input);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String val = input.getText().toString().trim();
            prefs.edit().putString(prop.valueKey(), val).apply();
            valueText.setText(val);
            Toast.makeText(this, prop.label + " 已更新", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // ─── 预设选择对话框 ───
    private void showPresetDialog() {
        String[] keys = PresetData.PRESETS.keySet().toArray(new String[0]);
        List<String> labelList = new ArrayList<>();
        for (String k : keys) {
            String lbl = PresetData.LABELS.get(k);
            labelList.add(lbl != null ? lbl : k);
        }
        String[] labels = labelList.toArray(new String[0]);

        new AlertDialog.Builder(this)
            .setTitle("选择预设机型")
            .setItems(labels, (dialog, which) -> {
                String presetKey = keys[which];
                prefs.edit().putString("preset", presetKey).apply();

                // 清空所有自定义值
                SharedPreferences.Editor ed = prefs.edit();
                for (PresetData.PropDef p : PresetData.PROPS) {
                    ed.remove(p.valueKey());
                }
                ed.apply();

                // 重建 UI
                updatePresetSummary();
                rebuildCards();
                Toast.makeText(this, "已切换为: " + labels[which], Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    // ─── 更新预设摘要 ───
    private void updatePresetSummary() {
        String presetKey = prefs.getString("preset", "oneplus_ace6t");
        String[] vals = PresetData.getPreset(presetKey);
        String brand = vals[PresetData.IDX_BRAND];
        String model = vals[PresetData.IDX_MODEL];
        tvPresetSummary.setText(brand + " " + model);

        // 从 LABELS 取显示名
        String label = PresetData.LABELS.get(presetKey);
        if (label != null) {
            tvPresetSummary.setText(label + "  (" + brand + " " + model + ")");
        }
    }

    private int dp(int px) {
        return (int) (px * getResources().getDisplayMetrics().density);
    }
}
