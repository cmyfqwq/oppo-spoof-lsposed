package com.oppo.spoof;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends Activity {

    private LinearLayout cardContainer;
    private SharedPreferences prefs;

    private TextView tvPresetLabel, tvPresetBrand, tvPresetModel, tvPresetAndroid, tvEnabledCount;

    private final List<Switch> switchList = new ArrayList<>();
    private int lastSavedScrollY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.primary_dark));

        prefs = getSharedPreferences("spoof_config", MODE_PRIVATE);
        cardContainer = findViewById(R.id.card_container);

        tvPresetLabel   = findViewById(R.id.tv_preset_label);
        tvPresetBrand   = findViewById(R.id.tv_preset_brand);
        tvPresetModel   = findViewById(R.id.tv_preset_model);
        tvPresetAndroid = findViewById(R.id.tv_preset_android);
        tvEnabledCount  = findViewById(R.id.tv_enabled_count);

        findViewById(R.id.btn_preset).setOnClickListener(v -> showPresetDialog());

        updatePresetSummary();
        rebuildCards(false);
    }

    // ─── 重建所有属性卡片 ───
    private void rebuildCards(boolean animate) {
        // 保存当前滚动位置
        View scrollParent = (View) cardContainer.getParent().getParent();
        if (scrollParent instanceof ScrollView) {
            lastSavedScrollY = scrollParent.getScrollY();
        }

        cardContainer.removeAllViews();
        switchList.clear();

        String[] presetVals = getCurrentPresetVals();
        int delayMs = 0;
        for (int i = 0; i < PresetData.PROPS.length; i++) {
            PresetData.PropDef prop = PresetData.PROPS[i];
            boolean isLast = (i == PresetData.PROPS.length - 1);
            View card = buildCard(prop, presetVals[prop.presetIdx], isLast);

            if (animate) {
                // 卡片依次滑入
                card.setAlpha(0f);
                card.setTranslationY(dp2px(24));
                card.animate().alpha(1f).translationY(0f)
                        .setDuration(220).setStartDelay(delayMs).start();
                delayMs += 40;
            }

            cardContainer.addView(card);
        }

        updateEnabledCount();

        // 恢复滚动位置
        if (scrollParent instanceof ScrollView) {
            ((ScrollView) scrollParent).post(() -> ((ScrollView) scrollParent).scrollTo(0, lastSavedScrollY));
        }
    }

    // ─── 获取当前预设值 ───
    private String[] getCurrentPresetVals() {
        return PresetData.getPreset(prefs.getString("preset", "oneplus_ace6t"));
    }

    // ─── 构建单张属性卡片 ───
    private View buildCard(PresetData.PropDef prop, String defaultVal, boolean isLast) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, isLast ? 0 : dp2px(10));
        card.setLayoutParams(cardParams);
        card.setBackgroundResource(R.drawable.card_bg);
        card.setPadding(dp2px(16), dp2px(14), dp2px(14), dp2px(14));

        // ── 第一行：图标 + 标签 + 开关 ──
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 图标圆形容器
        LinearLayout iconWrap = new LinearLayout(this);
        iconWrap.setGravity(Gravity.CENTER);
        iconWrap.setLayoutParams(new LinearLayout.LayoutParams(dp2px(36), dp2px(36)));
        iconWrap.setBackgroundResource(R.drawable.ic_bg_small);

        TextView icon = new TextView(this);
        icon.setText(prop.emoji);
        icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        icon.setGravity(Gravity.CENTER);
        iconWrap.addView(icon);
        topRow.addView(iconWrap);

        View gap1 = new View(this);
        gap1.setLayoutParams(new LinearLayout.LayoutParams(dp2px(12), 0));
        topRow.addView(gap1);

        TextView label = new TextView(this);
        label.setText(prop.label);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        label.setTextColor(getColor(R.color.text_primary));
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        topRow.addView(label);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
        topRow.addView(spacer);

        Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean(prop.enableKey(), false));
        sw.setOnCheckedChangeListener((v, checked) -> {
            prefs.edit().putBoolean(prop.enableKey(), checked).apply();
            updateCardAlpha(card, checked);
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
        valRowParams.setMargins(dp2px(48), dp2px(8), 0, 0);
        valueRow.setLayoutParams(valRowParams);
        valueRow.setClickable(true);
        valueRow.setFocusable(true);
        valueRow.setBackgroundResource(R.drawable.chip_value_bg);

        // 值文字
        TextView valueText = new TextView(this);
        valueText.setText(displayVal);
        valueText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        valueText.setTextColor(getColor(R.color.on_primary_container));
        valueText.setMaxLines(2);
        valueText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        valueText.setPadding(dp2px(10), dp2px(5), dp2px(2), dp2px(5));
        valueRow.addView(valueText);

        // 自定义标记
        if (savedVal != null && !savedVal.isEmpty()) {
            TextView editedMark = new TextView(this);
            editedMark.setText(" ✎");
            editedMark.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            editedMark.setTextColor(getColor(R.color.primary));
            valueRow.addView(editedMark);
        }

        card.addView(valueRow);

        // ── 点击事件（点击整行编辑，取代长按） ──
        Object[] tag = new Object[]{prop, valueText};
        valueRow.setTag(tag);
        valueRow.setOnClickListener(v -> showEditDialog(prop, valueText));

        // 初始透明度
        updateCardAlpha(card, sw.isChecked());

        return card;
    }

    // ─── 开关控制卡片透明度 ───
    private void updateCardAlpha(View card, boolean enabled) {
        card.setAlpha(enabled ? 1.0f : 0.55f);
    }

    // ─── 编辑对话框（含重置按钮） ───
    @SuppressWarnings("deprecation")
    private void showEditDialog(PresetData.PropDef prop, TextView valueText) {
        // 标题
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(dp2px(20), dp2px(20), dp2px(20), dp2px(8));

        TextView titleView = new TextView(this);
        titleView.setText(prop.emoji + "  " + prop.label);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTextColor(getColor(R.color.text_primary));
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        headerRow.addView(titleView);

        // 输入框
        String current = prefs.getString(prop.valueKey(), "");
        String presetVal = getCurrentPresetVals()[prop.presetIdx];
        String displayText = !current.isEmpty() ? current : presetVal;

        final EditText input = new EditText(this);
        input.setText(displayText);
        input.setSelectAllOnFocus(true);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        input.setTextColor(getColor(R.color.text_primary));
        input.setBackgroundColor(getColor(R.color.surface_variant));
        input.setPadding(dp2px(14), dp2px(14), dp2px(14), dp2px(14));
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp2px(20), dp2px(8), dp2px(20), dp2px(4));
        wrap.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 提示当前预设值
        if (!current.isEmpty()) {
            TextView hint = new TextView(this);
            hint.setText("预设值: " + presetVal);
            hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            hint.setTextColor(getColor(R.color.text_tertiary));
            hint.setPadding(dp2px(4), dp2px(6), 0, 0);
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

        // 重置按钮：清空自定义值回退到预设
        String hasCustom = prefs.getString(prop.valueKey(), null);
        if (hasCustom != null && !hasCustom.isEmpty()) {
            builder.setNeutralButton("重置为预设", (dialog, which) -> {
                prefs.edit().remove(prop.valueKey()).apply();
                rebuildCards(false);
                Toast.makeText(this, prop.label + " 已恢复预设值", Toast.LENGTH_SHORT).show();
            });
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // ─── 预设选择对话框 ───
    private void showPresetDialog() {
        String[] keys = PresetData.PRESETS.keySet().toArray(new String[0]);
        String currentKey = prefs.getString("preset", "oneplus_ace6t");
        String[] currentVals = PresetData.getPreset(currentKey);

        // 自定义布局：每行品牌·型号 | Android版本
        LinearLayout listView = new LinearLayout(this);
        listView.setOrientation(LinearLayout.VERTICAL);
        listView.setPadding(dp2px(8), dp2px(4), dp2px(8), dp2px(4));

        for (String k : keys) {
            String[] vals = PresetData.getPreset(k);
            String label = PresetData.LABELS.get(k);
            boolean isCurrent = k.equals(currentKey);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp2px(16), dp2px(14), dp2px(16), dp2px(14));
            row.setTag(k);

            // 选中标记
            TextView check = new TextView(this);
            check.setText(isCurrent ? "● " : "○ ");
            check.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            check.setTextColor(isCurrent ? getColor(R.color.success) : getColor(R.color.text_tertiary));
            check.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            row.addView(check);

            // 名称 + 型号
            LinearLayout mid = new LinearLayout(this);
            mid.setOrientation(LinearLayout.VERTICAL);
            mid.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView name = new TextView(this);
            name.setText(label);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            name.setTextColor(getColor(R.color.text_primary));
            name.setTypeface(Typeface.DEFAULT, isCurrent ? Typeface.BOLD : Typeface.NORMAL);
            mid.addView(name);

            TextView model = new TextView(this);
            model.setText(vals[PresetData.IDX_MODEL] + "  ·  Android " + vals[PresetData.IDX_RELEASE]);
            model.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            model.setTextColor(getColor(R.color.text_secondary));
            mid.addView(model);

            row.addView(mid);
            row.setClickable(true);
            row.setFocusable(true);
            row.setBackgroundResource(R.drawable.ripple_list_item);

            row.setOnClickListener(v -> {
                String presetKey = (String) v.getTag();
                // 关闭对话框
                if (v.getParent().getParent().getParent().getParent() instanceof AlertDialog) {
                    ((AlertDialog) v.getParent().getParent().getParent().getParent()).dismiss();
                }
                applyPreset(presetKey);
            });

            listView.addView(row);

            // 分隔线（最后一项不加）
            if (!k.equals(keys[keys.length - 1])) {
                View div = new View(this);
                div.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp2px(1)));
                div.setBackgroundColor(getColor(R.color.border));
                listView.addView(div);
            }
        }

        new AlertDialog.Builder(this)
            .setTitle("选择预设机型")
            .setView(listView)
            .create()
            .show();
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

    // ─── 工具方法 ───
    @SuppressWarnings("deprecation")
    private int getColor(int id) {
        return getResources().getColor(id, getTheme());
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }
}
