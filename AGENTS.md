# YoRemote 项目约定

- 本仓库是普通 Android 工程。直接修改根目录的 `app/`、`gradle/` 与 Gradle 配置，Android Studio 打开仓库根目录。
- `artifacts/` 是本地 APK、签名材料、历史构建和证据，不是当前开发入口。旧文档中的 src 子模块、补丁与重放命令已停用，不恢复这套流程。
- 版本号在 `app/build.gradle.kts` 中维护。namespace 为 `com.atharok.btremote`，当前优化测试包身份为 `io.github.yohuohuohuo.btremote.debug`；身份变化必须来自明确需求。
- 使用 JDK 21、Android SDK 36，直接执行 `./gradlew testDefaultDebugUnitTest assembleDefaultOptimized`。按改动范围补充 instrumentation 构建、lint 和真机回归。
- 本机覆盖安装沿用 `ANDROID_USER_HOME=$PWD/artifacts/.android-home` 下的原 Debug 密钥；禁止提交密钥、local.properties、缓存或 APK。不得覆盖旧 APK 或删除失败证据。
- 迁移前 lint 有 233 错误和 42 警告。以本轮实际结果为准，不关闭规则或添加宽泛 baseline 伪造通过。
- 保留 GPL-3.0 许可证和原作者声明。只提交相关改动，推送前检查暂存文件；提交后从干净 checkout 核对源码可用。
