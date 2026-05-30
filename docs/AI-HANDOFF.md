# AI 接手指南 (AI Handoff Guide)

## 项目概述

Fast QR Scan 是一个面向海外 Android 市场的二维码/条形码扫描、生成和 AI 美化工具。
使用 Kotlin + Jetpack Compose + Hilt + Room + CameraX 技术栈。

## 项目结构

```
QR/
├── app/                    # 应用壳模块（MainActivity、导航、主题）
├── core/
│   ├── common/            # 通用工具（日期格式化、分享、权限）
│   ├── data/              # 数据层（Room DB、加密、Repository 实现、DI）
│   ├── domain/            # 领域层（模型、接口、UseCase）
│   └── ui/                # 共享 UI（主题、通用组件、字符串）
├── feature/
│   ├── scanner/           # 扫码功能（CameraX + ML Kit）
│   ├── generator/         # QR 码生成（ZXing）
│   ├── history/           # 历史记录列表
│   ├── ai-workspace/      # AI 美化工作台
│   ├── onboarding/        # 新手引导
│   └── product-lookup/    # 商品查询
├── gradle/                # Gradle Wrapper + 版本目录
└── docs/                  # 文档
```

## 当前完成状态

### ✅ 已完成
- 完整的多模块项目架构
- 核心域模型和 Repository 接口/实现
- Room + SQLCipher 加密数据库
- Hilt 依赖注入配置
- Material 3 主题系统
- 3-Tab 底部导航（History/Scan/Create）
- Onboarding 引导流程（3 页）
- Scanner 模块（CameraX 预览、自动缩放、闪光灯控制、结果分类）
- Generator 模块（ZXing 编码、多类型输入、分辨率选择）
- History 模块（时间线列表、搜索、删除/收藏）
- AI Workspace 模块（颜色选择、点形状、实时预览）
- Product Lookup 模块（缓存优先策略、ViewModel）
- ProGuard 规则
- 应用图标（自适应图标）

### ❌ 待完成（后续版本）
- **网络层**：ProductApiService（Retrofit 接口）+ NetworkModule
- **WiFi 连接**：WifiNetworkSuggestion API 集成
- **vCard 保存**：解析 vCard 字段并创建联系人 Intent
- **相册导入**：ML Kit 对静态图片的条码解码
- **保存到相册**：MediaStore API 集成
- **分享图片**：FileProvider + ShareIntent
- **AI 模板**：云端 SD+ControlNet 模板（需要后端）
- **商业化**：广告 SDK + Google Play Billing（所有入口已用 TODO 标记）
- **隐私政策**：需要准备网页 URL

## 关键设计决策

1. **加密方案**：EncryptionKeyManager 使用"随机 passphrase + Keystore AES-GCM 加密存储"，
   不直接使用 Keystore key 的 encoded 属性（hardware-backed key 返回 null）

2. **导航三态逻辑**：MainNavHost 使用 null/true/false 三态避免首次启动闪烁

3. **跨模块依赖**：feature:ai-workspace 依赖 feature:generator（使用 QrEncoder）

4. **商业化占位**：所有未来商业化入口用 `// TODO [FUTURE-MONETIZATION]:` 标记

## 如何继续开发

### 编译运行
1. 用 Android Studio 打开项目根目录
2. 等待 Gradle Sync（首次约 5-10 分钟）
3. 如果缺少 gradle-wrapper.jar，Android Studio 会自动提示下载
4. 连接设备或启动模拟器，点击 Run

### 添加新功能
1. 查看 `.kiro/specs/qr-scan-max-app/tasks.md` 中的待办任务
2. 每个文件顶部都有"给其他 AI 开发者的说明"注释块
3. 搜索 `TODO [FUTURE` 查找所有预留的扩展点

### 签名发布
1. 复制 `app/keystore.properties.template` 为 `app/keystore.properties`
2. 生成 keystore：`keytool -genkey -v -keystore FastQrScan-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias FastQrScan`
3. 填入密码信息
4. 在 app/build.gradle.kts 中添加 signingConfigs 读取该文件

## 技术栈版本

| 技术 | 版本 |
|------|------|
| Kotlin | 2.0.0 |
| AGP | 8.5.0 |
| Compose BOM | 2024.06.00 |
| Hilt | 2.51.1 |
| Room | 2.6.1 |
| CameraX | 1.3.4 |
| ML Kit Barcode | 17.3.0 |
| ZXing | 3.5.3 |
| SQLCipher | 4.5.4 |
| Gradle | 8.7 |
| compileSdk | 35 |
| minSdk | 24 |
