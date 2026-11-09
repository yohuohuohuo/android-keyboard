# YoRemote 普通 Android 工程迁移计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在根仓库直接开发、构建和提交 YoRemote，取消子模块与补丁重放流程。

**Architecture:** 以已通过测试和真机反馈的 0.1.9 重放源码为迁移输入，将 app、Gradle 配置和 Wrapper 直接纳入根 Git。旧子模块、补丁和脚本移入本地忽略的备份目录；历史提交继续保留。

**Tech Stack:** Kotlin、Jetpack Compose、Gradle 9.5.0、AGP 9.2.1、JDK 21、Android SDK 36。

**Spec:** 用户本轮要求：YoRemote 代码移到普通目录，不再维护上游合并。沿用本聊天此前明确授权的主分支提交、推送和 APK/签名密钥保留要求。

## Global Constraints

- 0.1.9 / versionCode 17、namespace `com.atharok.btremote`、优化测试包 `io.github.yohuohuohuo.btremote.debug` 保持。
- 原 Debug 签名、GPL-3.0 许可证、作者声明和全部产品行为保持；不升级依赖。
- 保留所有旧 APK、密钥、历史失败证据；密钥与构建目录不入库。
- 不新建 worktree，直接在当前 main 执行；用户已选定迁移与收尾方向，无需额外设计/实施确认。

## Review Focus

- 迁移后源码/资源/Wrapper/ProGuard 是否与已验证输入逐字节一致。
- 根目录忽略规则是否正确排除构建缓存、APK、local.properties 与签名材料。
- checkout 后是否无需子模块/补丁即可运行 Gradle；CI 路径是否仍指向旧目录。
- 应用身份、签名与权限是否保持；迁移构建不能覆盖历史 APK。
- 历史文档与当前开发说明是否清楚区分，避免继续编辑 artifacts 或生成补丁。

## Task 1: 迁移与验证

**Files:** 根目录 app/、gradle/、gradlew、gradlew.bat、*.gradle.kts、gradle.properties；.gitignore、.github/workflows/apk.yml、README.md、AGENTS.md；删除 src gitlink、.gitmodules、patches/、res/、build.sh、generate.sh。

**Interfaces:** 开发和构建入口统一为根目录；`./gradlew testDefaultDebugUnitTest assembleDefaultOptimized`。应用接口不变。

- [x] 记录所有旧 APK 与密钥校验；验证迁移输入与远端重放结果一致。
- [x] 迁入应用/构建文件，归档旧工作流，更新忽略规则、CI 和中文开发说明。
- [x] 断言全部 237 个产品构建输入一致；运行原有 52 项测试、优化/Debug/instrumentation 构建及两种 lint。
- [x] 核对优化 APK 身份、签名、权限、ZIP 与旧文件保护结果；只检查迁移相关改动，不扩大产品复审。
- [x] 从待提交 Git 源码树的干净导出验证无子模块也可构建，完成源码、文档和证据核对。

Git 收尾沿用用户授权：提交当前 main、推送 origin/main，核验远端提交与工作区干净。实际提交与推送结果以 Git refs 和交付说明为准；构建输入在导出后未再修改，不为记录提交自身哈希额外修改文档。
