# 词映 / Ciying 0.3.2

Android 本地拼音输入法，在中文候选词右侧显示英文释义。Android 8.0 / API 26 及以上，目标 API 35。

## 这版可用的功能

首页同音示例为“公式 / formula、公示 / public notice”，试打提示为 `gongshi`。底部作者标记 `T.G` 或版本文字支持长按：先播放约 1.7 秒的原生光环、粒子与淡入动画，再只显示 `Terrence G`。已删除原先的两句说明及按钮文案；点背景、右上角 × 或返回键关闭。系统关闭动画时直接显示全名。

- 离线上下文排序：78,726 个词对，来自已有词库的弱搭配证据与词映原创分词例句；结合前文和本机选词习惯排序。不是神经模型，也不是从个人聊天记录训练。
- 有界单次拼写纠错：漏字母、多字母、相邻键误按、相邻字母颠倒；保留有效的精确读音，纠错候选明确标记“纠错”。例如 `shijain` → 时间。
- 密码、邮箱、数字输入适配，私密输入禁用个性化、上下文读取与选词记录；选中文本删除、码点退格、横屏非全屏键盘与编辑器动作适配。
- 新首页：授权雾林照片封面、启用状态卡、试打区与原创应用图标，键盘采用一致的森林配色。

- 全拼词库查询：265,799 条词音记录，205,926 条带英文释义；没有释义的词仍可输入。
- 同音候选：`tuoxie` 可选“拖鞋 / slippers”“妥协 / compromise”等。
- 拼音前缀补全、声母简拼（例如 `nh` → 你好）。
- 连续全拼，通过词图与一元词频束搜索组合候选，例如 `woxuexiyingyu` → 我学习英语。
- 选择前缀词后保留剩余拼音；空格选择候选；英文、数字、标点、退格、编辑器动作和换行。
- 选词频次仅保存在本机，用于同音词排序，最多 2,000 条；清除应用数据可删除。
- 词库复制、查询和组句在后台线程完成；过时查询结果不会覆盖新输入。

参考 [青简 Qingjian](https://github.com/qingjian-team/qingjian) 的分层架构，导入其基础词库、11 个领域词库与英文释义表，编写 Android/Kotlin 输入流程。这不是青简完整 Rust 引擎的 Android 移植：本版使用轻量词对加权和词图束搜索，没有移植青简训练的 bigram 文件或神经模型；双拼按用户要求不加入。尚无多次编辑纠错、模糊音设置、混合全拼/简拼、用户自造词及学习统计界面，复杂句子的首选仍可能不正确。英文释义为上游机器生成数据，未逐条人工校对。

## 安装与编译

### 从 GitHub 源码构建

安装 JDK 17 与 Android SDK（平台 android-35-ext15、Build Tools 35.0.0），设置 `ANDROID_HOME`，或在不提交的 `local.properties` 中设置 `sdk.dir`。使用 Gradle Wrapper：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
```

macOS / Linux 使用 `bash ./gradlew`。首次构建需要联网下载 Gradle 和 Maven 依赖；可添加 `-PciyingMirror=true` 使用项目配置的镜像。源码包含词库数据库、原始词库及转换脚本。SDK、JDK 和旧 APK 不随 GitHub 源码提供，下面的 `build-apk.ps1` 是原 U 盘环境的便捷脚本。

### 正式签名与升级

目前提供的是调试签名测试包。正式版需要创建专用签名密钥，用该密钥签署 release APK，并安全备份密钥与密码；后续更新持续使用同一密钥。签名表示软件发布身份，不等于应用商店审核。

密钥、密码、`keystore.properties` 和 `signing.properties` 不应提交到仓库。可在 Android Studio 通过 Build → Generate Signed Bundle / APK → APK 创建密钥并生成正式签名安装包。

正式签名与当前调试签名不同，通常无法直接覆盖已安装的测试版；首次切换需卸载测试版，卸载会清除本机选词偏好。正式版签名密钥应与源码分开保存，并至少保留一份安全备份。

最新版安装包：`Ciying-0.3.2-debug.apk`；SHA-256 在同名 `.sha256` 文件中。桌面副本名为 `词映-Ciying-0.3.2.apk`。以 BUILD-STATUS.md 记录的真机测试结果为准；旧版文件保留为历史版本。

项目附带 `tooling`、JDK 17、Gradle 8.9、Android SDK 35 Extension 15 和 Build Tools 35.0.0。在项目目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1 -PciyingMirror=true
```

脚本按当前盘符设置 SDK 和 JDK；Gradle 缓存默认放到电脑 `%LOCALAPPDATA%\Ciying\gradle-home`，可用 `CIYING_GRADLE_HOME` 覆盖。镜像参数可省略以使用官方仓库。输出为 `app\build\outputs\apk\debug\app-debug.apk`，是调试签名测试版，不是正式发布签名。

手机开启并授权 USB 调试后：

```powershell
.\android-sdk\platform-tools\adb.exe install -r .\app\build\outputs\apk\debug\app-debug.apk
.\android-sdk\platform-tools\adb.exe shell am start -n app.ciying.keyboard/.MainActivity
```

在应用中启用并切换到词映，再点击测试框。覆盖安装后如键盘服务未重连，先打开词映应用，切换到其他输入法再切回词映。

## 数据来源与重建

青简固定版本：`c97ff2b61933d6fcd845ee4f4f470faf3ea83bf6`。原始 TSV 保存在 `third_party/qingjian/`，来源、修改及各自许可见 [THIRD_PARTY.md](THIRD_PARTY.md) 和上游随附说明。代码采用 GPL-3.0-or-later，频率等数据同时保留各自原始许可；应用内可查看“开源与词库许可”。

使用带 sqlite3 的 Python 3 运行 `python scripts/build_lexicon.py` 重建 `app/src/main/assets/lexicon-v3.db`。脚本不联网，校验 SQLite 完整性、关键读音和释义，并将源数据（包括原创上下文例句）与产物 SHA-256、数量写入 `test-results/lexicon-build.json`。数据库构建放在电脑本地缓存后整块复制到 U 盘，避免 FAT32 的小文件写入瓶颈。数据发生变化时需同步升级 APK 内的数据库版本文件名，避免继续使用设备上的旧数据库。照片为 Kieren Ridley / Pexels 的雾林照片（5407567），许可、原始地址和用途见 THIRD_PARTY.md；不用于商标或应用图标。

## 测试

```powershell
.\build-apk.ps1 -PciyingMirror=true :app:testDebugUnitTest :app:lintDebug :smoke:assembleDebug
.\android-sdk\platform-tools\adb.exe install -r -t .\smoke\build\outputs\apk\debug\smoke-debug.apk
.\android-sdk\platform-tools\adb.exe shell am instrument -w app.ciying.keyboard.test/app.ciying.keyboard.KeyboardSmokeInstrumentation
.\android-sdk\platform-tools\adb.exe uninstall app.ciying.keyboard.test
```

手机必须解锁；测试页自动保持亮屏。独立 smoke 应用操作真实输入法，结果需包含全部 PASS 且没有 FAIL，不能只用 shell 退出码判断。0.3.0 新增错拼与上下文回归、密码/邮箱/数字框、搜索/发送/完成（只调用本地测试监听器，不发送消息）、表情与选区删除、横屏与真实 Android WebView 输入。测试后恢复自动旋转，并仅在词映首页处于前台时尝试保存首页截图。当前 16 项单元测试和 Redmi K50 Pro 上 28 项真机检查通过。小米密码框由系统安全键盘接管，测试尊重这一保护；未验证所有机型或实际微信会话。首页、键盘截图及完整结果见 test-results/ 和 BUILD-STATUS.md。
