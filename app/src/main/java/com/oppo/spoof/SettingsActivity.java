package com.oppo.spoof;

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
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends Activity {

    private LinearLayout cardContainer;
    private SharedPreferences prefs;

    private TextView tvPresetLabel;
    private TextView tvPresetBrand;
    private TextView tvPresetModel;
    private TextView tvPresetAndroid;
    private TextView tvEnabledCount;

    private final List<Switch> switchList = new ArrayList<>();
    private int lastScrollY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));

        prefs = getSharedPreferences("spoof_config", MODE_PRIVATE);
        cardContainer = findViewById(R.id.card_container);

        tvPresetLabel  = findViewById(R.id.tv_preset_label);
        tvPresetBrand  = findViewById(R.id.tv_preset_brand);
        tvPresetModel  = findViewById(R.id.tv_preset_model);
        tvPresetAndroid = findViewById(R.id.tv_preset_android);
        tvEnabledCount = findViewById(R.id.tv_enabled_count);

        findViewById(R.id.btn_preset).setOnClickListener(v -> showPresetDialog());

        updatePresetSummary();
        rebuildCards(false);
    }

    // ─── 重建所有属性卡片 ───
    private void rebuildCards(boolean animate) {
        // 保存滚动位置
        View scrollView = (View) cardContainer.getParent().getParent();
        if (scrollView instanceof ScrollView) {
            lastScrollY = scrollView.getScrollY();
        }

        cardContainer.removeAllViews();
        switchList.clear();

        String[] presetVals = getCurrentPresetVals();
        int delay = 0;
        for (int i = 0; i < PresetData.PROPS.length; i++) {
            PresetData.PropDef prop = PresetData.PROPS[i];
            boolean isLast = (i == PresetData.PROPS.length - 1);
            View card = buildCard(prop, presetVals[prop.presetIdx], isLast);

            if (animate) {
                card.setAlpha(0f);
                card.setTranslationY(dp(24));
                card.animate().alpha(1f).translationY(0f).setDuration(200).setStartDelay(delay);
                delay += 40;
            }

            cardContainer.addView(card);
        }

        updateEnabledCount();

        if (scrollView instanceof ScrollView) {
            ((ScrollView) scrollView).post(() -> ((ScrollView) scrollView).scrollTo(0, lastScrollY));
        }
    }

    // ─── 获取当前预设值 ───
    private String[] getCurrentPresetVals() {
        return PresetData.getPreset(prefs.getString("preset", "oneplus_ace6t"));
    }

    // ─── 构建单张属性卡片 ───
    @SuppressWarnings("deprecation")
    private View buildCard(PresetData.PropDef prop, String defaultVal, boolean isLast) {
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

        View gap1 = new View(this);
        gap1.setLayoutParams(new LinearLayout.LayoutParams(dp(12), 0));
        topRow.addView(gap1);

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
            card.setAlpha(checked ? 1.0f : 0.55f);
            updateEnabledCount();
        });
        switchList.add(sw);
        topRow.addView(sw);

        card.addView(topRow);

        // ── 第二行：值芯片 ──
        String savedVal = prefs.getString(prop.valueKey(), null);
        String displayVal = (savedVal != null && !savedVal.isEmpty()) ? savedVal : defaultVal;

        LinearLayout valueRow = new LinearLayout(this);
        valueRow.setOrientation(LinearLayout.HORIZONTAL);
        valueRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams valRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        valRowParams.setMargins(dp(48), dp(8), 0, 0);
        valueRow.setLayoutParams(valRowParams);
        valueRow.setClickable(true);
        valueRow.setFocusable(true);

        TextView valueText = new TextView(this);
        valueText.setText(displayVal);
        valueText.setTextSize(12f);
        valueText.setTextColor(getResources().getColor(R.color.on_primary_container));
        valueText.setBackgroundResource(R.drawable.chip_value_bg);
        valueText.setMaxLines(2);
        valueText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        valueText.setPadding(dp(10), dp(4), dp(10), dp(5));
        valueRow.addView(valueText);

        if (savedVal != null && !savedVal.isEmpty()) {
            TextView editedMark = new TextView(this);
            editedMark.setText(" ✎");
            editedMark.setTextSize(11f);
            editedMark.setTextColor(getResources().getColor(R.color.primary));
            valueRow.addView(editedMark);
        }

        card.addView(valueRow);

        // ── 点击编辑（单击触发） ──
        valueRow.setOnClickListener(v -> showEditDialog(prop, valueText));

        // 初始透明度
        card.setAlpha(sw.isChecked() ? 1.0f : 0.55f);

        return card;
    }

    // ─── 编辑对话框（含重置按钮） ───
    @SuppressWarnings("deprecation")
    private void showEditDialog(PresetData.PropDef prop, TextView valueText) {
        // 标题行
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(dp(20), dp(20), dp(20), dp(8));

        TextView titleView = new TextView(this);
        titleView.setText(prop.emoji + "  " + prop.label);
        titleView.setTextSize(18f);
        titleView.setTextColor(getResources().getColor(R.color.text_primary));
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        headerRow.addView(titleView);

        // 输入框
        String current = prefs.getString(prop.valueKey(), "");
        if (current.isEmpty()) {
            current = getCurrentPresetVals()[prop.presetIdx];
        }

        final EditText input = new EditText(this);
        input.setText(current);
        input.setSelectAllOnFocus(true);
        input.setTextSize(15f);
        input.setTextColor(getResources().getColor(R.color.text_primary));
        input.setBackgroundColor(getResources().getColor(R.color.surface_variant));
        input.setPadding(dp(14), dp(14), dp(14), dp(14));
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(20), dp(8), dp(20), dp(4));
        wrap.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 预设值提示
        String savedVal = prefs.getString(prop.valueKey(), null);
        if (savedVal != null && !savedVal.isEmpty()) {
            String presetVal = getCurrentPresetVals()[prop.presetIdx];
            TextView hint = new TextView(this);
            hint.setText("预设值: " + presetVal);
            hint.setTextSize(11f);
            hint.setTextColor(getResources().getColor(R.color.text_tertiary));
            hint.setPadding(dp(4), dp(6), 0, 0);
            wrap.addView(hint);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setCustomTitle(headerRow)
            .setView(wrap)
            .setPositiveButton("确定", (dialog, which) -> {
                String val = input.getText().toString().trim();
                if (!val.isEmpty()) {
                    prefs.edit().putString(prop.valueKey(), val).apply();
                    rebuildCards(false);
                    Toast.makeText(this, prop.label + " 已更新", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null);

        if (savedVal != null && !savedVal.isEmpty()) {
            builder.setNeutralButton("重置为预设", (dialog, which) -> {
                prefs.edit().remove(prop.valueKey()).apply();
                rebuildCards(false);
                Toast.makeText(this, prop.label + " 已恢复预设值", Toast.LENGTH_SHORT).show();
            });
        }

        builder.show();
    }

    // ─── 预设选择对话框（自定义布局） ───
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

            TextView check = new TextView(this);
            check.setText(isCurrent ? "● " : "○ ");
            check.setTextSize(16f);
            check.setTextColor(getResources().getColor(
                    isCurrent ? R.color.success : R.color.text_tertiary));
            row.addView(check);

            LinearLayout mid = new LinearLayout(this);
            mid.setOrientation(LinearLayout.VERTICAL);
            mid.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView name = new TextView(this);
            name.setText(label);
            name.setTextSize(15f);
            name.setTextColor(getResources().getColor(R.color.text_primary));
            name.setTypeface(Typeface.DEFAULT, isCurrent ? Typeface.BOLD : Typeface.NORMAL);
            mid.addView(name);

            TextView model = new TextView(this);
            model.setText(vals[PresetData.IDX_MODEL] + "  ·  Android "
                    + vals[PresetData.IDX_RELEASE]);
            model.setTextSize(12f);
            model.setTextColor(getResources().getColor(R.color.text_secondary));
            mid.addView(model);

            row.addView(mid);
            row.setClickable(true);
            row.setFocusable(true);

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
        rebuildCards(true);
        Toast.makeText(this, "已切换为: " + PresetData.LABELS.get(presetKey),
                Toast.LENGTH_SHORT).show();
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
