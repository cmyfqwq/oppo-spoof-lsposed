# MaskProps — 设备伪装模块

LSPosed/Xposed 框架模块，伪装设备 Build 属性为目标机型。勾选即生效，无需配置包名白名单。

## 功能

- 伪装 10 项系统属性：品牌 / 制造商 / 型号 / 设备代号 / 产品名 / 硬件 / 指纹 / Android 版本 / SDK / 显示ID
- 基于 **SystemProperties Hook**，绕过 ART 反射限制，兼容 Android 14 / 15 / 16
- 7 个预设机型，覆盖主流品牌：

| 预设 | 品牌 | 型号 |
|---|---|---|
| 一加 Ace 6T | OnePlus | PJF110 |
| 一加 13 | OnePlus | PJZ110 |
| OPPO Find X8 | OPPO | PKB110 |
| 小米 14 | Xiaomi | 23127PN0CC |
| 华为 Mate 60 Pro | HUAWEI | ALN-AL80 |
| 三星 S24 Ultra | samsung | SM-S9280 |
| vivo X100 Pro | vivo | V2324A |

- 桌面设置界面：每个属性独立开关控制，点击数值可自定义
- 预设数据集中维护（PresetData），添加新机型只需改一个文件

## 使用

1. 安装 LSPosed 框架（Magisk / KernelSU / APatch + Zygisk）
2. 安装本模块 APK
3. LSPosed → 模块 → 勾选「设备伪装」，选择作用域
4. 打开桌面图标，选择预设机型或单独调整属性
5. 重启目标 App 生效

## 编译

```bash
git clone https://github.com/cmyfqwq/oppo-spoof-lsposed.git
cd oppo-spoof-lsposed
./gradlew assembleRelease
```

Push 到 main 分支自动触发 GitHub Actions 编译。

## 架构

```
PresetData.java         — 共享数据源（预设机型 + 属性定义）
  ├── MainHook.java        — Xposed Hook（SystemProperties 拦截）
  └── SettingsActivity.java — 桌面 UI（切换预设 / 开关属性 / 编辑值）
```

配置存储：SharedPreferences + `XSharedPreferences.makeWorldReadable()` 跨进程读取。

## 下载

[Releases](https://github.com/cmyfqwq/oppo-spoof-lsposed/releases)

## 许可

MIT
