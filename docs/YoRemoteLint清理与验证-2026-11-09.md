# YoRemote 0.1.10 lint 清理与验证

日期：2026-11-09。用户授权修复剩余 233 个 lint 错误和 42 个警告，完成必要本地验证后提交并推送 main；保留已有 APK 和原签名密钥。本轮没有关闭 lint 规则、添加 baseline、降低严重度或新增 suppression。

## 结果

- `lintDefaultDebug`、`lintDefaultOptimized` 均通过，两个最终 XML 报告都包含 **0 个 issue：0 错误、0 警告**。
- 完整 **54 项测试通过，0 失败、0 错误、0 跳过**。其中原有 52 项保持，新增设置资源切换测试分别在 Robolectric API 36 和 37 执行。
- 普通 Debug、R8 优化包、Debug instrumentation APK 构建通过。instrumentation 只构建，未执行。
- 用户明确选择本轮先完成本地验证：未覆盖安装手机，未执行真机、蓝牙或 Android 17 实机回归。模拟 API 37 测试不能替代这些检查。

## 实际修复

| 原问题 | 修复 |
|---|---|
| MissingTranslation：227 错误 | 补全所有 20 个现有语言文件，共新增 1,119 条译文。删除无引用键后，每种语言覆盖 213 个必译键，无缺键、重复键、额外键或格式参数不匹配。未删除语言或复制英文占位填数。 |
| LocalContextGetResourceValueCall：6 错误 | 设置主题、音量动作转换回调在 composition 中使用 stringResource；键盘语言标签先按当前资源读取再排序；第三方链接先读取资源，再供点击回调使用，避免语言配置更新后保留旧值。 |
| UnusedResources：21 警告 | 删除 6 个无用颜色、15 个无用字符串及相应语言定义；核查源码/XML及动态查找路径，没有产品引用。 |
| 版本与目标 API：17 警告 | 更新官方稳定工具链、相关依赖与 SDK，见下表；没有使用动态版本。 |
| VectorRaster：1 警告 | 启动矢量图内在尺寸 288dp 改为 200dp，保留图形路径与 viewport。 |
| Typos：1 警告 | 将西语 HID 说明中的 Interface 改为相应西语表述。 |
| UnusedTranslation：1 警告 | 将补全后的 Breton 与 Danish 加入 localeConfig，保留原有语言。 |
| ObsoleteSdkInt：1 警告 | minSdk 已为 28，将两个自适应图标从 mipmap-anydpi-v26 移至 mipmap-anydpi，内容不变。 |
| 新版 Lifecycle 额外发现 EmptySuperCall：1 警告 | 移除父类明确为空的 super.onCleared()，保留 setInputsActive(false) 及原输入释放/取消逻辑。 |

`values/yoremote.xml` 的既有固定中文控制文案逐字节保持，其 translatable 约定未改。新增译文经过 XML、格式参数编号/类型/数量、换行和重复键检查及独立抽查；没有母语者人工审校，尤其 Breton 长帮助文本仍有措辞审校空间。

## 工具链与依赖

版本来自当前 Google Maven、Maven Central 和 Gradle 官方元数据；固定版本和原始查询结果保留在本轮证据目录。

| 项目 | 修复后 |
|---|---|
| Gradle / AGP / Kotlin | 9.9.0 / 9.5.1 / 2.4.21 |
| 构建 JDK / Java 与 Kotlin 字节码目标 | JDK 21 / 11 |
| compileSdk / targetSdk / minSdk | 37.2 / 37 / 28 |
| Core KTX / Lifecycle | 1.19.1 / 2.11.0 |
| Compose BOM / Navigation | 2026.09.00 / 2.10.2 |
| DataStore / Glance / WorkManager | 1.3.0 / 1.2.0 / 2.12.0 |
| Koin / kotlinx.serialization | 4.2.2 / 1.12.0 |
| Coroutines 运行时与测试 | 统一 1.11.0；显式声明应用实际使用的 Android 运行依赖，避免主包 1.9.0、测试 1.11.0 的验证偏差 |
| Robolectric / 单元测试 Espresso | 4.17 / 稳定版 3.7.0 |

新版 AndroidX 的内联字节码要求 JVM 11，已同步编译目标，没有提高最低 Android 版本。Robolectric 4.17 按其官方配置要求，仅对测试 JVM 开放 `java.base/jdk.internal.access`；没有更改应用权限、设备安全设置或系统沙箱。Compose 传递给单元测试的 Espresso 原为 3.5.0，补充已使用的稳定版 3.7.0，解决其调用 API 37 已移除内部方法的问题，未引入 alpha 依赖。

SDK 平台 37.2 通过 AGP 的正式 SDK 安装流程加入现有 SDK，旧平台保留。CI 明确安装 `platforms;android-37.2`，从根目录构建。Gradle 分发包 SHA-256 配置与 Wrapper JAR 均重新匹配官方校验值。

本机已安装 Android Studio 的 CFBundleShortVersionString 为 2025.1，尚未更新，不能将本轮命令行通过视为该旧 IDE 的同步通过。AGP 9.5 的 GUI 开发需支持它的 Android Studio 2026.2.2 或更新版本；本轮没有修改用户的 IDE 安装。JDK 21 命令行测试和构建已实际验证。[官方 IDE/AGP 兼容表](https://developer.android.com/build/releases/about-agp)。

[AGP 官方兼容说明](https://developer.android.com/build/releases/agp-9-5-0-release-notes)、[compileSdk minor API 配置](https://developer.android.com/reference/tools/gradle-api/9.5/com/android/build/api/dsl/CompileSdkSpec)、[Robolectric JDK 配置](https://robolectric.org/getting-started/)、[AndroidX Test 发布说明](https://developer.android.com/jetpack/androidx/releases/test)。

核对 [Android 17 目标行为变化](https://developer.android.com/about/versions/17/behavior-changes-17)：当前无 RFCOMM 读循环、MessageQueue 私有反射或静态 final 修改；Widget 图片为资源引用，没有发现新位图内存限制的直接触发路径。原有方向处理未变，也未使用 API 36 的大屏幕方向约束豁免。此为源码判断，不是 Android 17 实机验收。

## 回归测试与保留的失败证据

新增 SettingsResourcesTest 渲染真实设置页：保持 Context 为英文，将 LocalResources/LocalConfiguration 切换为法文，检查键盘语言、主题值和主题弹窗的标签随资源更新。原实现两个 SDK 都在法文键盘标签断言失败；修复后两例通过。

验证过程中发现测试将主题行滚到 y=0..66，点击中心被 y=0..64 的固定顶栏遮住，实际没有打开弹窗。最终以“外观”标题作滚动锚点再真实点击主题，保留全部资源与显示断言，没有 sleep、放宽断言、伪造 WindowInfo 或修改产品布局。首次失败及诊断 XML/log 全部保留。

首轮完整回归原 52 项均通过，新增两例仅在上述点击位置失败；最终完整回归为 54/54。字节码目标、Robolectric JVM 与 Espresso 解析问题的首错日志也保留，没有删除失败记录以声称通过。

执行命令（使用原 ANDROID_USER_HOME 与现有 Gradle 缓存）：

```sh
./gradlew --no-daemon --no-build-cache --continue \
  testDefaultDebugUnitTest lintDefaultDebug lintDefaultOptimized \
  assembleDefaultDebug assembleDefaultOptimized assembleDefaultDebugAndroidTest
```

完整运行退出码为 0。正常执行了两个完整 lint 任务；AGP 对重复的 lintVital 任务作 SKIPPED，不是关闭规则。Kotlin 编译日志仍有现有 Compose 测试 API 的弃用提示及测试环境日志，它们不属于本轮 Android Lint 的 42 项警告；没有把所有工具日志宣称为零警告。

## APK、签名与数据保护

- 新包：`artifacts/yoremote-0.1.10-optimized.apk`，5,072,671 字节（约 4.84 MiB）；版本 0.1.10-optimized / versionCode 18。
- 包名：`io.github.yohuohuohuo.btremote.debug`；namespace 保持 `com.atharok.btremote`；YoRemote 名称、minSdk 28、四种 ABI 保留。
- APK SHA-256：`392d7085fef7fd3a423d9b58b61bbeb52079fb8bc8b857ea60f440f949883fc2`；独立 `.sha256` 文件随包保存在本地。
- 原 Debug 证书 SHA-256：`6296b9bd847b163d05efb0d89861f67ca46983a9e522a79183818ae573da501e`，v2 签名通过、非调试状态保持，可供用户之后覆盖安装。该证书仍只定位于本地测试。
- ZIP CRC、16 KiB 原生库/4 字节 ZIP 对齐核对通过；最终 Manifest 的全部组件类均在优化 DEX 中。权限集合与旧优化包相同，未新增 INTERNET；未把 Espresso、JUnit、Robolectric 或 Compose tooling 放进优化 APK。
- 12 个旧交付 APK、原 SHA256SUMS 与签名密钥的内容校验全部不变。另将正常构建会覆盖的 3 个旧临时 APK 先备份到 `previous-build-apks/`，没有丢弃历史包。
- 新包由本轮已验证源码在提交前构建，源文件指纹随证据保存；APK 的 Git 元数据记录当时父提交，不能将其混同为最终提交号。最终提交保存同一套产品输入。

按最终 lint 模型中的实际运行依赖及 3 个构建插件查询 OSV，共 125 个公开 Maven 坐标，未命中已收录公告；未上传源码、APK、输入内容或凭据。查询不覆盖未知漏洞、全部测试工具传递依赖或 Android 系统组件，不构成绝对安全保证。

## 证据与边界

本轮证据在忽略的 `artifacts/yoremote-lint-evidence/`：原始与最终 XML/HTML/TXT/SARIF、最终完整构建日志、54 项测试 XML、RED/GREEN 与弹窗诊断、语言完整性检查、官方版本元数据、SDK/Wrapper 校验、运行依赖解析、OSV 响应、APK/Manifest/签名检查及旧文件保护校验。

限范围独立复核未发现可行动问题；后续仅复核了空 super 调用、测试滚动锚点与 Coroutines 对齐，未做新的全局产品扫描。`git diff --check` 通过，Windows Wrapper 脚本沿用仓库原 LF 风格。

未执行真机/蓝牙回归、Android 17 实机、instrumentation 运行或远端 Release 发布工作流。源码与本报告正常提交到 main；推送一致性及最终提交号以 Git refs 和交付说明为准。
