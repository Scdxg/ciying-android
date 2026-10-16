# 词映构建记录

## 当前交付：0.3.2

作者彩蛋改为原生 Canvas 光环与粒子展开，约 850ms 后开始淡入 Terrence G，全程约 1.7 秒。最终仅保留全名与关闭按钮，无原先两句文案或“收到这份心意”按钮。点空白、× 或返回键关闭；系统关闭动画时直接显示全名，View 销毁时停止动画。

APK 编译、lint、签名验证通过，Redmi K50 Pro 已覆盖安装，长按底部 T.G 后实际 UI 确认只出现 Terrence G 与 ×，旧文案未出现，返回键关闭正常。记录：test-results/creator-0.3.2-ui.txt。桌面副本 C:/Users/Administrator/Desktop/词映-Ciying-0.3.2.apk 与项目 Ciying-0.3.2-debug.apk 校验值一致。输入引擎未改动，未重复整套输入回归。

## 0.3.1 历史记录

首页同音示例改为“公式 / formula、公示 / public notice”，试打提示换为 gongshi。页脚增加金色 T.G 标记，长按 T.G 或版本文字弹出 Terrence G 作者彩蛋。

assembleDebug、lintDebug 和 APK 签名验证通过；Redmi K50 Pro 覆盖安装成功。读取实际 UI 确认新示例显示，长按 T.G 后确认出现 Terrence G 弹窗，文本记录位于 test-results/creator-0.3.1-ui.txt。输入引擎未改动，本次不重复执行上一版的整套输入回归。

安装包保存在 U 盘根项目 Ciying-0.3.1-debug.apk，另复制至 C:/Users/Administrator/Desktop/词映-Ciying-0.3.1.apk，两份 SHA-256 一致；同名校验文件保存在项目目录。

## 0.3.0 历史记录

2026-10-16 新增轻量离线上下文词对排序、单次编辑拼写纠错、密码/邮箱/数字与横屏等输入适配。78,726 个词对由既有词库搭配和词映原创例句生成，可复现脚本与校验值随项目保存；未移植青简的训练模型，双拼按用户要求不加入。

首页更换为 Kieren Ridley / Pexels 授权雾林照片，配套森林色卡片、原生试打区和原创矢量应用图标；照片来源及许可在应用内和 THIRD_PARTY.md 中提供。

16 项单元测试通过（13 个转换器用例、3 个输入框策略用例），lint 0 错误、4 警告。最终 0.3.0（含私密模式增强）已覆盖安装；根目录 Ciying-0.3.0-debug.apk 与测试构建 SHA-256 一致，签名验证通过，APK 包含新数据库、照片和矢量图标。

用户解锁后，在 Redmi K50 Pro 完成 28 项真机检查，结果无 FAIL：基础输入与词库、shijain → 时间（标记纠错）、参加 + shijian → 参加实践、私密模式同一上下文 → 参加时间（不使用上下文与个人偏好）、数字与邮箱框、搜索/发送/完成动作（仅本地监听器，不发送消息）、emoji 码点删除与选区删除、横屏、Android WebView。密码框由小米系统安全键盘接管，已确认接管并保留系统保护，没有禁用安全键盘；词映自身密码策略另有 JVM 测试覆盖。本轮未进入微信实际会话，不能声称已验证所有聊天 App 或所有机型。

测试结果：test-results/smoke-0.3.0-results.txt。已取得并目视检查首页与键盘截图：test-results/ciying-home-0.3.0.png、ciying-keyboard-0.3.0.png，未发现文字遮挡或按键越界。截图预览模式只操作词映自己的试打框。临时测试应用已移除，系统旋转已恢复自动，屏幕超时保持测试前的原值 600000 ms，词映仍为当前输入法。

## 当前交付：0.2.0（以下旧版记录保留作历史）

2026-10-16 完成词库与转换模块升级，APK：`Ciying-0.2.0-debug.apk`。青简来源固定为 c97ff2b61933d6fcd845ee4f4f470faf3ea83bf6；基础与 11 个领域词库合并去重后 265,799 条词音记录，其中 205,926 条带英文释义；完整来源与修改见 THIRD_PARTY.md。

已实现全拼查询、前缀补全、声母简拼、一元词频词图束搜索连续输入、部分选词消费、同音选择本地频次排序、后台查询与过时结果丢弃。未移植青简 Rust 引擎、bigram/神经模型、纠错与双拼，不能将本版称为完整青简移植。

验证：SQLite integrity_check 与关键读音/释义校验通过；7 项引擎单元测试通过；最新 lintDebug 0 错误、6 警告；APK v2 签名验证通过。Redmi K50 Pro 已覆盖安装同一 APK；12 个真机输入框断言全部通过，包括 tuoxie → 拖鞋（slippers）/妥协（compromise）、woxuexiyingyu → 我学习英语、nh → 你好，以及原有字母、退格、选词、空格、中英、数字、换行测试。结果见 test-results/smoke-0.2.0-results.txt。临时测试应用已卸载，词映保持当前输入法。

应用内“开源与词库许可”可查看许可，原始数据、重建脚本和校验记录随项目提供。旧 0.1.0 APK 保留，新版请使用 0.2.0。

日期：2026-10-16（香港时间）。

- 已生成 `Ciying-0.1.0-debug.apk`（调试签名），包名 `app.ciying.keyboard`，最低 Android 8.0/API 26，目标 API 35。
- `assembleDebug` 成功；`lintDebug` 成功；`apksigner verify --verbose` 验证通过（v2 签名）。SHA-256 见同名 `.sha256` 文件。
- 已修正 SDK 旧路径、SDK Extension 目录、Build Tools 版本与 Java/Kotlin JVM 目标不一致的问题。
- 已修正窄屏键盘按键宽度、切换输入框时拼音状态清空、回车按下/释放与编辑器动作处理，增加应用内测试输入框。
- 便携编译入口：`build-apk.ps1`，可选 `-PciyingMirror=true`。缓存默认在电脑本地，源码与 APK 保存在 U 盘。
- Redmi K50 Pro 真机：ADB 调试连接成功；`adb install -r` 返回 Success；MainActivity 冷启动成功（298 ms）；已启用并设为当前输入法，系统确认测试 EditText 获得焦点及 `mInputShown=true`。
- 候选显示、点击上屏与完整交互测试尚未完成：检查画面时 USB 断开，随后 ADB 设备列表为空，需重新连接后继续。未取得有效键盘截图。
- 当前词库仍是小型演示词库，未实现完整拼音或整句输入。

下一步真机验证：安装 APK → 启用并切换词映 → 应用内测试框输入 ni/hao/xuexi/yingyu → 检查中英候选与点击上屏 → 检查退格、空格、回车、切换输入框与窄屏显示 → 检查崩溃日志。

## 键盘修复与布局更新

- 修复字母键点击时调用 `TextView.append` 导致按键文字叠加的问题，改为独立的 `appendPinyin` 输入函数和显式操作回调。
- 布局调整为四排 QWERTY：第二排缩进，第三排右侧删除键，底部宽空格、中英切换、数字符号、标点及绿色编辑器动作键；浅灰底白色圆角按键，固定 48dp 按键高度。
- 空格选择第一候选；删除使用 Unicode 码点；数字符号和英文模式可用。
- 新 APK 编译、lintDebug 均成功，新布局 APK 已在 Redmi K50 Pro 覆盖安装成功，根目录 APK 与 SHA-256 已更新。
- 增加平台 API 真机测试 runner，覆盖字母输入不改写键帽、退格、中英候选、点击上屏、空格选词、英文、数字、回车。
- 自动测试尚未执行：测试 APK 三次安装均返回 INSTALL_FAILED_USER_RESTRICTED；随后确认手机处于 Asleep 且 Keyguard showing=true。需用户解锁并允许测试 APK 安装后运行，不能据此宣称交互测试通过。

后续进展：用户开启 USB 安装后，测试 APK 已安装成功。测试页启动方式已修正为 shell 启动与 Activity 生命周期回调；但运行时手机锁屏（Keyguard showing=true），未完成按键断言。runner 已加入锁屏检查、测试页保持亮屏和重新选择词映输入法。待用户解锁后继续；测试完成后卸载 `app.ciying.keyboard.test`，保留词映本体。

## 最新结果：真机完整测试通过

2026-10-16 在 Redmi K50 Pro 上，以独立 `smoke` 测试应用操作真实词映输入法：8 个输入框断言全部通过，覆盖 s 字母输入、拼音退格、ni → 你 / you 候选并点击上屏、hao + 空格 → 你好、已提交中文退格、中英切换与英文输入、数字符号输入、回车换行。测试记录：`test-results/smoke-results.txt`。

测试发现并修复多行编辑器中回车键标签不遵循 IME_FLAG_NO_ENTER_ACTION 的问题；最新 :app:lintDebug 成功，APK 签名验证通过。根目录 APK 和 SHA-256 均已更新，手机已安装同一份最新 APK，临时测试应用已移除，词映本体保留并选为当前输入法。

测试框架最终改为独立 smoke 应用（包名 app.ciying.keyboard.test），避免直接 instrumentation 输入法包造成服务绑定干扰。覆盖安装后曾出现服务未重连，切换到系统原有输入法再切回词映后完成全部测试。尚未验证更多手机、横屏及完整拼音输入；目前仍是演示词库。
