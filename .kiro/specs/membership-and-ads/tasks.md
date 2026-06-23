# Implementation Plan: 会员体系与广告体系

## Overview

本实现计划将会员订阅和广告变现体系拆分为自底向上的开发任务：先定义 domain 层数据模型与接口，再实现 core:billing 和 core:ads 基础模块，接着构建 feature:subscription 订阅页面，最后在 app 层完成启动编排与各 feature 模块的广告集成。

## Tasks

- [x] 1. 定义 domain 层数据模型与抽象接口
  - [x] 1.1 在 core:domain 中创建订阅相关数据模型
    - 创建 `SubscriptionState` 密封类（Loading / Free / Premium）
    - 创建 `SubscriptionPlan` 枚举（TRIAL / WEEKLY / ANNUAL / LIFETIME），含 productId、basePlanId、offerId、isOneTime 字段
    - 创建 `PurchaseResult` 密封类（Success / Cancelled / Error）
    - _Requirements: 1.1, 1.3, 1.4_

  - [x] 1.2 在 core:domain 中创建 SubscriptionRepository 接口
    - 定义 `subscriptionState: StateFlow<SubscriptionState>`
    - 定义 `isPremium: StateFlow<Boolean>`
    - 定义 `queryPurchases()`, `launchPurchaseFlow()`, `restorePurchases()` 方法
    - _Requirements: 1.2, 12.1, 12.4_

  - [x] 1.3 在 core:domain 中创建广告相关数据模型与接口
    - 创建 `AdType` 枚举（APP_OPEN / INTERSTITIAL / NATIVE）
    - 创建 `AdPlacement` 枚举（含各场景广告单元 ID 配置）
    - 创建 `AdShowResult` 密封接口（Shown / LoadFailed / FrequencyLimited / PremiumUser）
    - 创建 `NativeAdState` 密封接口（Loading / Loaded / Failed / Hidden）
    - 创建 `AdManager` 接口（shouldShowAd / preload / showFullScreenAd / getNativeAdState）
    - 创建 `FrequencyController` 接口（canShowInterstitial / isWithinSessionLimit / recordInterstitialShow / resetSession）
    - _Requirements: 10.1, 10.2, 2.1_

  - [x] 1.4 在 core:domain 中创建 ListAdInserter 工具类
    - 创建 `ListItem<T>` 密封类（Content / AdSlot）
    - 实现 `insertAds(items, interval)` 方法，按固定间隔插入广告位
    - _Requirements: 9.1_

  - [ ]* 1.5 编写 ListAdInserter 属性测试
    - **Property 6: 历史列表广告插入位置正确性**
    - 使用 Kotest Property 验证：Content 项相对顺序不变、相邻 AdSlot 间恰有 K 个 Content 项、Content 总数等于 N
    - **Validates: Requirements 9.1**

- [x] 2. 实现 core:billing 模块
  - [x] 2.1 创建 core:billing 模块骨架与 Gradle 配置
    - 新建 `core/billing` 目录，创建 `build.gradle.kts`
    - 在 `settings.gradle.kts` 中注册模块 `:core:billing`
    - 添加 Google Play Billing Library 依赖（billing-ktx）
    - 在 `libs.versions.toml` 中添加 billing 版本目录
    - 配置模块依赖：core:domain、core:data、Hilt
    - _Requirements: 1.2_

  - [x] 2.2 实现 SubscriptionPreferences 本地缓存
    - 使用 DataStore Preferences 实现 `isPremium`、`activePlan`、`expiryTimeMs` 的读写
    - 实现 `updateStatus()` 方法用于更新缓存
    - _Requirements: 12.1, 12.2_

  - [x] 2.3 实现 PlayBillingClientWrapper
    - 封装 BillingClient 的连接、查询商品详情、发起购买流程
    - 实现连接超时（5 秒）和自动重连逻辑
    - 实现购买结果回调解析
    - _Requirements: 1.2, 11.3_

  - [x] 2.4 实现 SubscriptionStateManager
    - 实现 `refreshState()` 从 Google Play 查询并更新状态
    - 实现 `onPurchaseCompleted()` 处理购买成功
    - 实现 `onSubscriptionExpired()` 处理过期回退
    - 失败时使用 DataStore 缓存状态降级
    - _Requirements: 1.3, 1.4, 12.1, 12.2_

  - [x] 2.5 实现 SubscriptionRepositoryImpl
    - 实现 `SubscriptionRepository` 接口
    - 注入 PlayBillingClientWrapper 和 SubscriptionStateManager
    - 实现 `launchPurchaseFlow()` 和 `restorePurchases()`
    - 使用 Hilt `@Binds` 注入
    - _Requirements: 1.2, 11.3, 12.4_

  - [ ]* 2.6 编写 SubscriptionPlan 属性测试
    - **Property 1: 订阅方案目录完整性**
    - 验证 SubscriptionPlan.values() 恰好 4 个方案，每个 productId 非空，LIFETIME 的 isOneTime 为 true
    - **Validates: Requirements 1.1**

  - [ ]* 2.7 编写 SubscriptionStateManager 属性测试
    - **Property 2: 订阅状态机正确性**
    - 使用随机事件序列验证：购买确认→Premium，过期/取消→Free，恢复购买有效→Premium
    - **Validates: Requirements 1.3, 1.4, 12.2**

  - [ ]* 2.8 编写 core:billing 单元测试
    - 测试购买成功状态转换
    - 测试过期回退逻辑
    - 测试 DataStore 缓存读取降级
    - _Requirements: 1.3, 1.4, 12.1_

- [x] 3. Checkpoint - 确保 domain 与 billing 模块测试通过
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. 实现 core:ads 模块
  - [x] 4.1 创建 core:ads 模块骨架与 Gradle 配置
    - 新建 `core/ads` 目录，创建 `build.gradle.kts`
    - 在 `settings.gradle.kts` 中注册模块 `:core:ads`
    - 添加 Google AdMob SDK 依赖（play-services-ads）
    - 在 `libs.versions.toml` 中添加 admob 版本目录
    - 配置模块依赖：core:domain、core:data、Hilt
    - _Requirements: 2.1_

  - [x] 4.2 配置广告单元 ID（BuildConfig）
    - 在 `app/build.gradle.kts` 中为 debug/release 配置所有广告场景的 BuildConfig 字段
    - debug 使用 Google 官方测试广告 ID
    - release 预留真实广告 ID 占位
    - _Requirements: 6.3, 7.1, 3.2, 4.1, 5.1, 9.1, 9.2_

  - [x] 4.3 实现 FrequencyControllerImpl
    - 实现 60 秒最小间隔控制
    - 实现单次会话最多 10 次展示限制
    - 实现 `recordInterstitialShow()` 和 `resetSession()`
    - _Requirements: 10.1, 10.2, 10.3_

  - [ ]* 4.4 编写 FrequencyController 属性测试
    - **Property 5: 插屏广告频率控制**
    - 使用随机时间戳序列验证：两次允许展示间隔 ≥ 60秒，单次会话总展示 ≤ 10 次
    - **Validates: Requirements 10.1, 10.2, 10.3**

  - [x] 4.5 实现 AdManagerImpl（开屏广告与插屏广告）
    - 实现 `shouldShowAd()` — Premium 用户直接返回 false
    - 实现 `preload()` — 预加载指定场景广告
    - 实现 `showFullScreenAd()` — 展示开屏/插屏广告，含 10 秒超时
    - _Requirements: 2.1, 3.2, 6.3, 6.5, 7.1, 7.3_

  - [x] 4.6 实现 AdManagerImpl（原生广告）
    - 实现 `getNativeAdState()` — 返回原生广告状态 StateFlow
    - 实现原生广告加载逻辑
    - 加载失败时返回 Hidden 状态，不影响布局
    - _Requirements: 4.1, 4.3, 5.1, 5.3, 9.1, 9.4_

  - [x] 4.7 实现 AdGatekeeper 广告拦截协调器
    - 注入 AdManager、SubscriptionRepository、FrequencyController
    - 实现 `gate()` 方法：先展示订阅页，关闭后按频率控制决定是否展示插屏
    - 返回 `Proceed` 或 `SubscriptionPurchased`
    - _Requirements: 6.1, 6.2, 6.3, 8.1, 8.2_

  - [ ]* 4.8 编写 AdManager shouldShowAd 属性测试
    - **Property 3: Premium 用户全局绕过所有限制**
    - 遍历所有 AdPlacement，验证 Premium 用户时 shouldShowAd 始终返回 false
    - **Validates: Requirements 2.1, 2.2, 2.3**

  - [ ]* 4.9 编写 AdGatekeeper 拦截决策属性测试
    - **Property 4: 免费用户受控操作拦截**
    - 验证 Free 用户触发受控操作时，拦截决策逻辑返回"需要拦截"
    - **Validates: Requirements 6.1, 6.2, 8.1**

  - [ ]* 4.10 编写 core:ads 单元测试
    - 测试广告加载失败降级（跳过广告继续操作）
    - 测试频率控制边界条件
    - 测试 Premium 用户跳过广告
    - _Requirements: 6.5, 7.3, 10.3_

- [x] 5. Checkpoint - 确保 core:ads 模块测试通过
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. 实现 feature:subscription 模块
  - [x] 6.1 创建 feature:subscription 模块骨架与 Gradle 配置
    - 新建 `feature/subscription` 目录，创建 `build.gradle.kts`
    - 在 `settings.gradle.kts` 中注册模块 `:feature:subscription`
    - 配置模块依赖：core:domain、core:billing、core:ui、Hilt、Compose
    - _Requirements: 11.1_

  - [x] 6.2 实现 SubscriptionViewModel
    - 注入 SubscriptionRepository
    - 暴露 `plans: StateFlow<List<SubscriptionPlanUiModel>>` 展示方案列表
    - 暴露 `purchaseState: StateFlow<PurchaseUiState>` 管理购买流程状态
    - 实现 `selectPlan()`, `confirmPurchase()`, `restorePurchases()` 方法
    - _Requirements: 11.1, 11.3, 11.4, 11.5, 12.3, 12.4_

  - [x] 6.3 实现 SubscriptionScreen Composable
    - 展示所有订阅方案（名称、价格、权益描述）
    - 提供关闭按钮允许跳过
    - 提供"恢复购买"按钮
    - 选中方案后展示确认购买按钮
    - 展示购买成功/失败状态反馈
    - _Requirements: 11.1, 11.2, 11.4, 11.5, 12.3_

  - [ ]* 6.4 编写 SubscriptionViewModel 单元测试
    - 测试方案列表正确加载
    - 测试购买成功后状态转换
    - 测试购买取消/失败的 UI 状态
    - _Requirements: 11.3, 11.4, 11.5_

- [x] 7. 实现启动编排与 App 层集成
  - [x] 7.1 实现 StartupOrchestrator
    - 注入 SubscriptionRepository 和 AdManager
    - 暴露 `startupState: StateFlow<StartupState>`
    - 实现 `orchestrate()` 方法：查询订阅状态→Premium 直接进首页 / Free 展示订阅页→关闭后展示开屏广告→进入首页
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x] 7.2 修改 app 模块 build.gradle.kts 添加新模块依赖
    - 添加 `implementation(project(":core:billing"))`
    - 添加 `implementation(project(":core:ads"))`
    - 添加 `implementation(project(":feature:subscription"))`
    - _Requirements: 1.2, 2.1_

  - [x] 7.3 修改 MainNavHost 集成启动流程
    - 在 Onboarding 完成后，集成 StartupOrchestrator 的启动状态判断
    - 根据 startupState 展示订阅页 / 开屏广告 / 直接进入首页
    - 添加 SubscriptionScreen 导航路由
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x] 7.4 集成扫描结果拦截（feature:scanner）
    - 在扫描结果回调前调用 AdGatekeeper.gate()
    - 免费用户先展示订阅页，关闭后按频率控制展示插屏广告
    - Premium 用户直接展示结果
    - _Requirements: 6.1, 6.3, 6.4, 6.5_

  - [x] 7.5 集成生成操作拦截（feature:generator）
    - 在生成二维码/条码操作前调用 AdGatekeeper.gate()
    - 免费用户先展示订阅页，关闭后按频率控制展示插屏广告
    - Premium 用户直接执行生成
    - _Requirements: 6.2, 6.3, 6.4, 6.5_

  - [x] 7.6 集成 AI 美化页面插屏广告
    - 在导航至 AI 美化页面前，展示插屏广告
    - 广告完成/失败后继续导航
    - Premium 用户直接进入
    - _Requirements: 7.1, 7.2, 7.3_

  - [x] 7.7 集成高级功能广告解锁流程
    - 展示"需观看广告解锁"提示
    - 确认后展示插屏广告，成功后解锁功能
    - 失败时展示重试提示
    - 取消时保持锁定
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [ ]* 7.8 编写 StartupOrchestrator 单元测试
    - 测试 Premium 用户冷启动直接进首页
    - 测试 Free 用户冷启动→订阅页→关闭→开屏广告→首页
    - 测试开屏广告加载失败直接进首页
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 8. 集成原生广告到各页面
  - [x] 8.1 集成 Onboarding 新手引导页原生广告
    - 在引导页面中嵌入 NativeCardAd Composable
    - 使用 NATIVE_ONBOARDING 场景
    - 加载失败时正常继续引导流程
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 8.2 集成 Home Tab 栏原生广告
    - 在底部 Tab 导航栏上方添加 NativeCardAd Composable（高度 ≤ 80dp）
    - 使用 NATIVE_HOME_TAB 场景
    - Premium 用户隐藏广告区域
    - 加载失败时隐藏广告区域
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 8.3 集成历史记录列表广告
    - 使用 ListAdInserter 在历史记录列表中按间隔插入广告位
    - 使用 NATIVE_HISTORY_LIST 场景
    - 加载失败时隐藏对应广告位
    - _Requirements: 9.1, 9.3, 9.4_

  - [x] 8.4 集成扫描结果详情页原生广告
    - 在扫描结果详情页内容区域嵌入 NativeCardAd Composable
    - 使用 NATIVE_SCAN_RESULT_DETAIL 场景
    - 加载失败时隐藏广告区域
    - _Requirements: 9.2, 9.3, 9.4_

  - [ ]* 8.5 编写原生广告集成单元测试
    - 测试 Premium 用户不展示原生广告
    - 测试加载失败时 Hidden 状态
    - _Requirements: 2.1, 9.4_

- [x] 9. 配置 AdMob 初始化与 AndroidManifest
  - [x] 9.1 在 AndroidManifest.xml 中添加 AdMob App ID meta-data
    - 添加 `com.google.android.gms.ads.APPLICATION_ID` meta-data
    - debug 使用测试 App ID，release 使用真实 App ID
    - _Requirements: 2.1_

  - [x] 9.2 在 Application 类中初始化 MobileAds SDK
    - 在 FastQrScanApplication 中调用 `MobileAds.initialize()`
    - 配置测试设备 ID（debug 模式）
    - _Requirements: 2.1_

- [x] 10. Final Checkpoint - 确保所有模块编译通过且测试通过
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- 任务标记 `*` 的为可选测试子任务，可在 MVP 阶段跳过
- 每个任务引用具体需求编号以确保可追溯性
- 属性测试验证设计文档中定义的通用正确性属性
- 单元测试验证具体示例和边界条件
- Checkpoints 确保增量验证，避免问题积累
- 广告 ID 使用 BuildConfig 区分 debug/release 环境
- 所有新模块遵循项目已有的 Hilt + 多模块架构约定

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["1.4", "2.1", "4.1"] },
    { "id": 2, "tasks": ["1.5", "2.2", "2.3", "4.2"] },
    { "id": 3, "tasks": ["2.4", "4.3"] },
    { "id": 4, "tasks": ["2.5", "2.6", "2.7", "4.4", "4.5"] },
    { "id": 5, "tasks": ["2.8", "4.6", "4.7"] },
    { "id": 6, "tasks": ["4.8", "4.9", "4.10", "6.1"] },
    { "id": 7, "tasks": ["6.2", "6.3"] },
    { "id": 8, "tasks": ["6.4", "7.1", "7.2"] },
    { "id": 9, "tasks": ["7.3", "9.1", "9.2"] },
    { "id": 10, "tasks": ["7.4", "7.5", "7.6", "7.7"] },
    { "id": 11, "tasks": ["7.8", "8.1", "8.2", "8.3", "8.4"] },
    { "id": 12, "tasks": ["8.5"] }
  ]
}
```
