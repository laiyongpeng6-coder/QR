# 需求文档：会员体系与广告体系

## 简介

本功能为 QRScanFast Android 应用引入会员订阅体系和广告变现体系。会员用户享有解锁全部功能且无广告的权益；非会员用户可使用所有功能，但需观看广告或在特定触发点展示广告。广告类型包含开屏广告、插屏广告和原生卡片广告，通过 Google AdMob SDK 接入；订阅通过 Google Play Billing Library 实现。

## 术语表

- **Subscription_Service**：负责管理会员订阅购买、验证和状态查询的服务模块
- **Ad_Manager**：负责广告加载、展示和生命周期管理的服务模块
- **Subscription_Screen**：展示订阅方案并引导用户购买的全屏订阅页面
- **App_Open_Ad**：应用从后台回到前台或冷启动时展示的开屏广告
- **Interstitial_Ad**：全屏插屏广告，在特定操作节点展示
- **Native_Card_Ad**：嵌入界面内以卡片形式展示的原生广告
- **Premium_User**：已订阅任一会员方案的用户
- **Free_User**：未订阅任何会员方案的用户
- **Trial_Plan**：3 天免费试用后自动转为 $6.99/周续费的订阅方案
- **Weekly_Plan**：$6.99/周的订阅方案
- **Annual_Plan**：$16.99/年的订阅方案
- **Lifetime_Plan**：$19.99 一次性购买的终身会员方案
- **Onboarding_Flow**：新用户首次使用应用时的引导流程
- **Home_Screen**：包含底部 Tab 导航栏的应用主界面
- **Advanced_Feature**：需要观看广告后才能解锁使用的高级功能（如 AI 美化等）

## 需求

### 需求 1：订阅方案配置

**用户故事：** 作为产品运营人员，我希望系统支持多种订阅方案，以便为用户提供灵活的会员选择。

#### 验收标准

1. THE Subscription_Service SHALL 支持以下订阅方案：3 天试用后自动转为 $6.99/周续费的 Trial_Plan、$6.99/周的 Weekly_Plan、$16.99/年的 Annual_Plan、$19.99 一次性购买的 Lifetime_Plan
2. THE Subscription_Service SHALL 通过 Google Play Billing Library 管理所有订阅方案的购买和验证
3. WHEN 用户完成订阅购买, THE Subscription_Service SHALL 在 3 秒内更新用户的会员状态为 Premium_User
4. WHEN 用户的订阅过期或被取消, THE Subscription_Service SHALL 将用户状态回退为 Free_User

### 需求 2：会员权益

**用户故事：** 作为付费会员，我希望解锁所有功能且不再看到广告，以获得流畅的使用体验。

#### 验收标准

1. WHILE 用户为 Premium_User 状态, THE Ad_Manager SHALL 停止加载和展示所有广告
2. WHILE 用户为 Premium_User 状态, THE App SHALL 允许用户直接使用所有 Advanced_Feature 而无需观看广告
3. WHILE 用户为 Premium_User 状态, THE Subscription_Screen SHALL 不再展示给该用户

### 需求 3：首启订阅页与开屏广告

**用户故事：** 作为产品运营人员，我希望在应用首次启动时向用户展示订阅页，以提高转化率；如用户关闭订阅页则展示开屏广告。

#### 验收标准

1. WHEN 应用冷启动且用户为 Free_User, THE App SHALL 首先展示 Subscription_Screen
2. WHEN Free_User 关闭 Subscription_Screen（未完成订阅）, THE Ad_Manager SHALL 展示 App_Open_Ad
3. WHEN App_Open_Ad 展示完成或被用户关闭, THE App SHALL 导航至 Home_Screen
4. WHEN 应用冷启动且用户为 Premium_User, THE App SHALL 直接导航至 Home_Screen

### 需求 4：新手引导流程原生广告

**用户故事：** 作为产品运营人员，我希望在新手引导流程中展示原生广告，以在不影响体验的前提下增加广告收入。

#### 验收标准

1. WHILE Free_User 处于 Onboarding_Flow, THE Ad_Manager SHALL 在引导页面中嵌入 Native_Card_Ad
2. THE Native_Card_Ad SHALL 以卡片形式展示，视觉风格与引导页面保持一致
3. IF Native_Card_Ad 加载失败, THEN THE Onboarding_Flow SHALL 正常继续而不展示广告占位

### 需求 5：首页原生广告

**用户故事：** 作为产品运营人员，我希望在首页 Tab 栏上方展示原生卡片广告，以增加广告曝光。

#### 验收标准

1. WHILE Free_User 处于 Home_Screen, THE Ad_Manager SHALL 在底部 Tab 导航栏上方展示 Native_Card_Ad
2. THE Native_Card_Ad SHALL 以卡片形式展示，高度不超过 80dp，不遮挡主要内容区域
3. IF Native_Card_Ad 加载失败, THEN THE Home_Screen SHALL 隐藏广告区域且不影响布局

### 需求 6：扫描和生成时的订阅页与插屏广告

**用户故事：** 作为产品运营人员，我希望在用户执行扫描或生成二维码操作时展示订阅页和插屏广告，以在高频场景中提高订阅转化或广告收入。

#### 验收标准

1. WHEN Free_User 触发扫描操作（获取扫描结果）, THE App SHALL 首先展示 Subscription_Screen
2. WHEN Free_User 触发生成二维码/条码操作, THE App SHALL 首先展示 Subscription_Screen
3. WHEN Free_User 关闭 Subscription_Screen（未完成订阅）, THE Ad_Manager SHALL 展示 Interstitial_Ad
4. WHEN Interstitial_Ad 展示完成或被用户关闭, THE App SHALL 继续执行用户原本的操作
5. IF Interstitial_Ad 加载失败, THEN THE App SHALL 直接继续执行用户原本的操作而不阻塞

### 需求 7：AI 美化页面插屏广告

**用户故事：** 作为产品运营人员，我希望在用户进入 AI 美化页面前展示插屏广告，以在高价值功能入口增加收入。

#### 验收标准

1. WHEN Free_User 尝试进入 AI 美化页面, THE Ad_Manager SHALL 展示 Interstitial_Ad
2. WHEN Interstitial_Ad 展示完成或被用户关闭, THE App SHALL 导航用户进入 AI 美化页面
3. IF Interstitial_Ad 加载失败, THEN THE App SHALL 直接导航用户进入 AI 美化页面

### 需求 8：高级功能广告解锁

**用户故事：** 作为普通用户，我希望通过观看广告来解锁高级功能，以免费体验全部能力。

#### 验收标准

1. WHEN Free_User 尝试使用 Advanced_Feature, THE App SHALL 展示提示告知用户需观看广告后解锁
2. WHEN Free_User 确认观看广告, THE Ad_Manager SHALL 展示 Interstitial_Ad
3. WHEN Interstitial_Ad 展示完成, THE App SHALL 解锁对应的 Advanced_Feature 供用户使用
4. IF Free_User 取消观看广告, THEN THE App SHALL 保持 Advanced_Feature 锁定状态
5. IF Interstitial_Ad 加载失败, THEN THE App SHALL 展示加载失败提示并允许用户重试

### 需求 9：其他界面原生卡片广告

**用户故事：** 作为产品运营人员，我希望在应用的其他合适界面中嵌入原生卡片广告，以增加整体广告收入。

#### 验收标准

1. WHILE Free_User 处于历史记录页面, THE Ad_Manager SHALL 在列表中每隔固定条目嵌入 Native_Card_Ad
2. WHILE Free_User 处于扫描结果详情页, THE Ad_Manager SHALL 在页面内容区域展示 Native_Card_Ad
3. THE Native_Card_Ad SHALL 以卡片形式展示，视觉风格与所在页面保持一致
4. IF Native_Card_Ad 加载失败, THEN THE App SHALL 隐藏对应广告区域且不影响页面正常功能

### 需求 10：广告频率控制

**用户故事：** 作为用户，我希望广告展示有合理的频率限制，以避免过于频繁的广告干扰使用体验。

#### 验收标准

1. THE Ad_Manager SHALL 控制 Interstitial_Ad 的展示间隔不少于 60 秒
2. THE Ad_Manager SHALL 在单次会话中限制 Interstitial_Ad 展示总次数不超过 10 次
3. WHEN 广告展示间隔未满足条件, THE Ad_Manager SHALL 跳过广告直接执行后续操作

### 需求 11：订阅页面交互

**用户故事：** 作为用户，我希望订阅页面清晰展示各方案信息并支持便捷购买，以便做出购买决策。

#### 验收标准

1. THE Subscription_Screen SHALL 展示所有可用订阅方案的名称、价格和权益描述
2. THE Subscription_Screen SHALL 提供关闭按钮允许用户跳过订阅
3. WHEN 用户选择一个订阅方案并确认购买, THE Subscription_Service SHALL 调用 Google Play Billing 发起购买流程
4. WHEN 购买成功, THE Subscription_Screen SHALL 关闭并提示购买成功
5. IF 购买失败或被用户取消, THEN THE Subscription_Screen SHALL 展示对应错误信息并保持在当前页面

### 需求 12：订阅状态持久化与恢复

**用户故事：** 作为用户，我希望更换设备或重新安装应用后能恢复我的会员状态，以避免重复购买。

#### 验收标准

1. WHEN 应用启动, THE Subscription_Service SHALL 通过 Google Play Billing 查询当前用户的有效订阅状态
2. WHEN 查询到有效订阅, THE Subscription_Service SHALL 将用户状态设为 Premium_User
3. THE Subscription_Screen SHALL 提供"恢复购买"按钮
4. WHEN 用户点击"恢复购买", THE Subscription_Service SHALL 查询并恢复用户的历史购买记录
