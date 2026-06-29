# OPPO Spoof — LSPosed 设备伪装模块

LSPosed 框架模块，伪装设备 Build 信息为目标机型。**LSPosed 勾选即生效，无需配置包名白名单**。

## 功能

- 🎭 伪装 10 项设备属性：品牌 / 制造商 / 型号 / 设备代号 / 产品名 / 硬件 / 指纹 / 版本号 / SDK / 显示ID
- 🔧 核心采用 **SystemProperties Hook**，兼容 Android 14/15/16（绕过 ART 反射封锁）
- 📱 7 个预设机型：一加 Ace 6T / 一加 13 / OPPO Find X8 / 小米 14 / 华为 Mate 60 Pro / 三星 S24 Ultra / vivo X100 Pro
- 🎛️ 桌面设置界面：独立开关控制每个属性，支持自定义覆盖值
- 🏗️ **PresetData 共享架构**：预设数据集中维护，MainHook 和 UI 自动同步

## 使用方法

1. 安装 **LSPosed** 框架（Magisk/KernelSU/APatch + Zygisk）
2. 安装本模块 APK
3. LSPosed → 模块 → 勾选「设备伪装」，选择需要伪装的 App
4. 打开「设备伪装」桌面图标，选择预设机型或自定义属性
5. 重启目标 App 生效

## 编译

```bash
git clone https://github.com/cmyfqwq/oppo-spoof-lsposed.git
cd oppo-spoof-lsposed
./gradlew assembleRelease
```

GitHub Actions 自动编译：push 到 main 分支即自动构建 APK。

## 架构

```
PresetData.java      ← 共享预设数据（单一数据源）
    ├── MainHook.java       ← Xposed Hook 逻辑（SystemProperties 拦截）
    └── SettingsActivity.java ← 桌面设置 UI（卡片式界面）
```

配置存储：SharedPreferences（应用内 `MODE_PRIVATE`，跨进程 `XSharedPreferences.makeWorldReadable()`）


## 下载

前往 [Releases](https://github.com/cmyfqwq/oppo-spoof-lsposed/releases) 页面下载最新 APK。

## 许可证

MIT
