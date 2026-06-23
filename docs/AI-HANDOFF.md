# AI 接手指南

这份文档是 Fast QR Scan 的总接力入口。任何人类或 AI 在开始改代码前，先读这里，再读目标模块的文件头注释和任务文档。

## 1. 项目概述

Fast QR Scan 是一个面向海外 Android 市场的二维码 / 条形码扫描、生成和 AI 美化工具。
技术栈是 Kotlin + Jetpack Compose + Hilt + Room + CameraX。

## 2. 项目结构

```text
QR/
├── app/                    # 应用壳模块（MainActivity、导航、主题）
├── core/
│   ├── common/             # 通用工具（日期格式化、分享、权限）
│   ├── data/               # 数据层（Room DB、加密、Repository 实现、DI）
│   ├── domain/             # 领域层（模型、接口、UseCase）
│   └── ui/                 # 共享 UI（主题、通用组件、字符串）
├── feature/
│   ├── scanner/            # 扫码功能（CameraX + ML Kit）
│   ├── generator/          # QR 码生成（ZXing）
│   ├── history/            # 历史记录列表
│   ├── ai-workspace/       # AI 美化工作台
│   ├── onboarding/         # 新手引导
│   └── product-lookup/     # 商品查询
├── gradle/                 # Gradle Wrapper + 版本目录
└── docs/                   # 文档
```

## 3. 当前完成状态

### 已完成

- 完整的多模块项目架构（app / core / feature 六模块）
- 核心域模型和 Repository 接口 / 实现
- Room + SQLCipher 加密数据库
- Hilt 依赖注入配置
- Material 3 主题系统（支持 Light / Dark）
- 3-Tab 底部导航（History / Scan / Create）
- Onboarding 引导流程（3 页品牌化卡片布局，含 Icon + 渐变装饰）
- Scanner 模块（CameraX 预览、自动缩放、闪光灯控制、结果分类）
- Generator 模块（ZXing 编码、多类型输入、分辨率选择）
- History 模块（时间线列表、搜索、删除 / 收藏）
- AI Workspace 模块（颜色选择、点形状、实时预览）
- Product Lookup 模块（缓存优先策略、ViewModel）
- ProGuard 规则
- 应用图标（自适应图标）
- Firebase Analytics + Crashlytics 集成
- Settings 设置页面（自动打开URL / 扫码震动 / 语言切换 / 反馈 / 隐私政策 / 服务条款）
- 多语言国际化：英文（默认）+ 中文简体 + 中文繁体 + 德语 + 西班牙语 + 葡萄牙语
- 应用内语言切换（AppCompat per-app language）
- 版本号已升至 1.0.3（versionCode = 4）

### 待完成

- 网络层：`ProductApiService` + `NetworkModule`（Retrofit 已声明但未实现）
- WiFi 连接：`WifiNetworkSuggestion` 集成
- vCard 保存：解析 vCard 并创建联系人 Intent
- 相册导入：ML Kit 对静态图片的条码解码
- 保存到相册：MediaStore API 集成
- 分享图片：FileProvider + ShareIntent
- AI 模板：云端 SD + ControlNet 模板
- 商业化：广告 SDK + Google Play Billing
- 隐私政策：准备实际网页 URL
- 文档：ARCHITECTURE.md、CONTRIBUTING.md、ROADMAP.md 尚未创建

## 4. 关键设计决策

1. 加密方案：`EncryptionKeyManager` 使用“随机 passphrase + Keystore AES-GCM 加密存储”，不直接使用 Keystore key 的 `encoded` 属性。
2. 导航三态逻辑：`MainNavHost` 使用 `null / false / true` 三态避免首次启动闪烁。
3. 跨模块依赖：`feature:ai-workspace` 依赖 `feature:generator`，复用 `QrEncoder`。
4. 商业化占位：所有未来商业化入口统一使用 `// TODO [FUTURE-MONETIZATION]:`。
5. UI 统一入口：尽量把通用颜色、排版、按钮、卡片、加载态放到 `core/ui`。
6. Onboarding 视觉：不再使用 Drawable 插图，改用 Material Icon + 品牌化渐变卡片设计（参见 `OnboardingPage` 模型的 `icon`、`accentColor`、`accentColorSecondary` 字段）。
7. Firebase 集成：Analytics + Crashlytics 用于崩溃追踪和用户行为分析（不发送 PII）。
8. 设置页面：直接放在 `app` 模块的 `settings/` 包中，通过导航路由 `NavRoutes.Settings` 进入。
9. 多语言策略：使用 AppCompat `AppCompatDelegate.setApplicationLocales` 实现应用内语言切换，支持 6 种语言。

## 5. 开工前必须读什么

1. 先读这份文档。
2. 再读目标模块的 `ViewModel`、`Screen`、`model`、`repository` 文件。
3. 再读 `.kiro/specs/qr-scan-max-app/tasks.md` 里对应任务。
4. 再搜索 `TODO [FUTURE` 找到未来扩展点。
5. 如果是 UI 任务，再先看 `core/ui/theme` 和同模块其他页面的风格。

## 6. 文件头注释规则

每个可独立开发的文件，建议保留“给其他 AI 开发者的说明”块，里面至少包含：

- 这个文件的职责
- 当前做到哪一步
- 以后谁来接
- 不能随便改的地方
- 和其他模块的依赖关系
- 关键 TODO

推荐优先维护这些文件：

- `ViewModel`
- `Screen`
- `model`
- `repository`
- `controller`
- `core/ui` 里的通用组件

## 7. 交接规则

每次让 AI 接手时，建议按这个顺序给任务：

1. 目标是什么。
2. 现在卡在哪里。
3. 哪些文件允许改。
4. 哪些文件不能动。
5. 接受什么样的完成结果。
6. 需要哪些测试或截图。

每次 AI 完成后，建议回填：

- 已完成内容
- 未完成内容
- 风险点
- 测试结果
- 下一步建议

## 8. 任务切分原则

- 一次只让 AI 改一个主题。
- UI 和业务尽量不要混在同一轮里大改。
- 先做骨架，再做细节。
- 先改共享组件，再改业务页面。
- 先做单页，再做全局联动。

## 9. 多语言协同规则

这个项目是多语言应用，凡是涉及功能、文案、页面、按钮、空状态、错误提示、引导页内容调整，都必须先考虑多语言协同。

具体规则如下：

- 先改资源文件，再改页面代码。
- 不允许把正式文案直接硬编码在 Composable 里，除非是临时调试。
- 新增文案键时，必须同步检查现有语言目录是否需要补齐或调整。
- 如果只改布局不改文案，也要确认不会破坏现有翻译键和语义。
- 如果界面调整会影响长度、断行或按钮宽度，要同时检查中英文和其他语言的表现。
- 对外可见的提示语、错误语、空状态、引导语，都算在多语言协同范围内。

## 10. 新功能标准流程

新功能不要直接开写代码，按这个顺序走：

1. 用户场景
2. 状态模型
3. 页面骨架
4. 导航入口
5. 数据流 / 业务逻辑
6. 交互细节
7. 空状态 / 错误状态
8. 动效和视觉优化
9. 测试

这部分的完整执行版放在 `docs/FEATURE-WORKFLOW.md`。

## 11. 编译运行

1. 用 Android Studio 打开项目根目录。
2. 等待 Gradle Sync。
3. 连接设备或启动模拟器。
4. 点击 Run。

## 12. 添加新功能

1. 先看 `.kiro/specs/qr-scan-max-app/tasks.md`。
2. 再看 `docs/FEATURE-WORKFLOW.md`。
3. 再看目标模块的文件头说明。
4. 再搜索 `TODO [FUTURE` 查找扩展点。

## 13. 签名发布

1. 复制 `app/keystore.properties.template` 为 `app/keystore.properties`。
2. 生成 keystore：`keytool -genkey -v -keystore FastQrScan-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias FastQrScan`
3. 填入密码信息。
4. 在 `app/build.gradle.kts` 中添加 signingConfigs 读取该文件。

## 14. 技术栈版本

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
| Firebase BOM | 33.1.0 |
| Retrofit | 2.11.0 |
| OkHttp | 4.12.0 |
| Coil | 2.7.0 |
| AppCompat | 1.7.0 |
| Gradle | 8.7 |
| compileSdk | 35 |
| minSdk | 24 |
| targetSdk | 35 |
| versionCode | 4 |
| versionName | 1.0.3 |

## 15. 给 AI 的推荐提示词

```text
你正在接手 Fast QR Scan Android 项目。
先阅读 docs/AI-HANDOFF.md 和目标模块文件头注释，再开始修改。
请先输出：
1. 你理解的目标
2. 你准备修改的文件
3. 你认为的风险
4. 你的实现顺序

如果是 UI 任务，请先给出页面结构与视觉方向，不要直接写完整代码。
如果是业务任务，请先给出状态和数据流，不要直接把 UI 与业务混在一起。
```
