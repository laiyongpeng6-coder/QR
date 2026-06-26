# Fast QR Scan - Barcode Reader

Android QR 码/条码扫描与生成应用，包含 AI 美化、会员订阅和广告变现体系。

## 功能特性

- 📷 **高速扫描** — CameraX + ML Kit，支持 QR Code、EAN-13、Code 128 等 10+ 格式
- 🎨 **二维码生成** — 支持文本、URL、WiFi、联系人等多种内容类型
- ✨ **AI 美化** — 自定义配色、点形状等样式编辑
- 📋 **历史记录** — SQLCipher 加密本地存储，支持搜索和收藏
- 🌐 **多语言** — 英文、简体中文、繁体中文、德语、西班牙语、葡萄牙语
- 💎 **会员订阅** — Google Play Billing（3天试用/周/年/终身）
- 📺 **广告变现** — AdMob（开屏/插屏/原生卡片，9个场景独立 ID）
- 🔧 **远程控制** — Firebase Remote Config 动态管理广告开关和参数

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin 2.0 |
| UI | Jetpack Compose + Material3 |
| 架构 | 多模块 Clean Architecture（MVVM） |
| DI | Hilt |
| 数据库 | Room + SQLCipher |
| 相机 | CameraX |
| 扫码 | ML Kit Barcode Scanning |
| 生码 | ZXing |
| 网络 | Retrofit + OkHttp |
| 计费 | Google Play Billing 7.0 |
| 广告 | AdMob (play-services-ads 23.1) |
| 分析 | Firebase Analytics + Crashlytics |
| 配置 | Firebase Remote Config |
| 构建 | AGP 8.5.2, Gradle Kotlin DSL |

## 模块结构

```
app/                    → 应用壳（Activity、导航、DI、启动编排）
core/
  domain/               → 纯 Kotlin 领域模型和接口
  data/                 → Room 数据库、DataStore、Repository 实现
  billing/              → Google Play Billing 封装
  ads/                  → AdMob SDK 封装、频率控制、广告拦截器
  ui/                   → 共享 Compose 组件和主题
  common/               → 工具类（Analytics、LocaleManager、RemoteConfig）
feature/
  scanner/              → 扫码功能（CameraX + ML Kit）
  generator/            → 生码功能（ZXing）
  history/              → 历史记录
  ai-workspace/         → AI 美化工作台
  onboarding/           → 新手引导
  subscription/         → 订阅页面
  product-lookup/       → 商品查询
```

## 构建运行

```bash
# 克隆项目
git clone https://github.com/laiyongpeng6-coder/QR.git
cd QR/QRAdriord

# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease

# 运行测试
./gradlew test
```

## 环境要求

- Android Studio Ladybug+
- JDK 17
- Android SDK: compileSdk 35, minSdk 24
- 需要 `google-services.json`（Firebase 配置文件，不包含在仓库中）

## Remote Config 参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `ads_enabled` | Boolean | 全局广告开关（审核期设为 false） |
| `ads_firstpay_show` | Boolean | 首启是否展示订阅页 |
| `ads_max_per` | Number | 单次会话插屏广告上限 |
| `ads_show_sec` | Number | 插屏广告最小间隔（秒） |
| `mustUpdate_version` | String | 强制更新最低版本号 |

## 许可证

私有项目，未经授权不得复制或分发。
