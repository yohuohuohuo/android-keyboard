# YoRemote

YoRemote 将 Android 手机作为蓝牙 HID 触控板、键盘和遥控器，直接控制已配对的 Mac 或其他支持蓝牙输入的设备，无需安装电脑接收端。

现在是普通 Android 工程：**Android Studio 直接打开本仓库根目录，修改 `app/` 后正常提交 Git。** 不再使用子模块、补丁生成或源码重放。

## 代码与版本

- 应用源码和资源：`app/src/main/`；测试：`app/src/test/`、`app/src/androidTest/`。
- 版本号、包名、构建类型和签名配置：`app/build.gradle.kts`。
- 依赖版本：`gradle/libs.versions.toml`；Wrapper：`gradle/wrapper/`。
- 当前版本：0.1.10 / versionCode 18；显示名：YoRemote。
- namespace：`com.atharok.btremote`；基础 applicationId：`io.github.yohuohuohuo.btremote`。
- 日常测试用 `defaultOptimized`：包名带 `.debug`，使用 Debug 证书，开启 R8 和资源裁剪，关闭调试与 Debug tooling。

`artifacts/` 保存本地 APK、签名材料、历史构建及证据，全部被 Git 忽略。不要在那里继续开发，也不要向 Git 提交密钥或 APK。

## 开发与构建

需要 JDK 21、Android SDK 37.2 和 Build Tools 36。当前使用 Gradle 9.9.0、AGP 9.5.1、Kotlin 2.4.21，目标 Android API 37、最低 API 28。Android Studio 打开根目录并完成 SDK 配置后，在根目录执行：

使用 IDE 同步时，需要支持 AGP 9.5 的 Android Studio（2026.2.2 或更新版本）；旧版 IDE 不影响已配置 JDK/SDK 的命令行构建。

```sh
./gradlew --no-daemon testDefaultDebugUnitTest assembleDefaultOptimized
```

优化 APK：`app/build/outputs/apk/default/optimized/app-default-optimized.apk`。

本机需要覆盖现有安装时，继续使用原 Debug 密钥。macOS 命令行示例：

```sh
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_USER_HOME="$PWD/artifacts/.android-home"
./gradlew --no-daemon testDefaultDebugUnitTest assembleDefaultOptimized
```

在本机 Codex 沙箱中可复用已有缓存：`GRADLE_USER_HOME=/private/tmp/android-keyboard-gradle-01a1ba37`。其他开发环境可使用 Gradle 默认缓存。原密钥仅在本地；重新克隆不会带回它，覆盖安装必须使用原证书。

按改动范围补充普通 Debug、instrumentation APK、lint 和真机检查：

```sh
./gradlew --no-daemon assembleDefaultDebug assembleDefaultDebugAndroidTest
./gradlew --no-daemon --continue lintDefaultDebug lintDefaultOptimized
```

lint 的当前验收标准为 Debug 和 Optimized 两种变体均 0 错误 / 0 警告，不关闭规则或添加 baseline。模拟测试不能代替蓝牙、输入法、入口复用和异常断连的真机验证。

## 提交与推送

1. 直接修改根目录下的源码、资源、测试或 Gradle 文件。版本号只在 `app/build.gradle.kts` 中维护。
2. 完成与本次改动相关的测试、构建和必要真机回归，将需保留的 APK 另存到 `artifacts/`，不覆盖旧包。
3. 用 `git status` 和 `git diff` 检查改动，暂存后检查 `git diff --cached`，确认没有密钥、local.properties、缓存或 APK。
4. 正常提交并推送到目标分支；本项目主分支为 `main`。源码本身随提交保存，无需再导出补丁。
5. 交付前确认远端提交一致；需要可复现性验证时，在干净 checkout 中直接运行 Gradle，不执行子模块或补丁命令。

GitHub 发布工作流也从根目录直接构建。正式 Release 仍需独立配置发行签名，不要把 Debug 证书当作正式发行密钥。

## 记录与来源

[0.1.10 lint 清理与验证](docs/YoRemoteLint清理与验证-2026-11-09.md) 记录两种变体的零 lint 结果、54 项测试、SDK/依赖更新和本地新包，当前开发配置以本轮为准。

[目录迁移记录](docs/YoRemote普通工程迁移-2026-11-09.md) 是当前工程结构的说明。[0.1.9 体积优化与安全审查](docs/YoRemote体积优化与安全审查-2026-11-09.md) 保留当时的 APK、测试和安全证据；其中 src、补丁、重放命令均是迁移前历史，不再是当前开发流程。

原有四页为“触控板、键盘、快捷输入、遥控”，默认触控板。中文使用手机发送英文/拼音、Mac 日常中文输入法组字，未增加联网、剪贴板同步或电脑接收端。

项目基于 [Atharok 的 BT Remote](https://gitlab.com/Atharok/BtRemote) 与 [jqssun/android-bt-remote](https://github.com/jqssun/android-bt-remote) 定制。历史固定来源为 `56ea84e6b8f16e45c6467768b72e1bcd8f322f72`，旧结构完整保留在本仓库 Git 历史中；今后独立维护 YoRemote，不要求合并回上游。

遵循 [GPL-3.0 许可证](LICENSE)，保留原作者与源文件版权声明。`fastlane/` 为原有商店资料，不是构建输入。
