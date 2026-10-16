# 正式签名构建

签名密钥和密码不保存在源码仓库中。构建配置默认读取仓库同级 `Ciying-Signing-Backup/signing.properties`，也可通过环境变量 `CIYING_SIGNING_PROPERTIES` 或 Gradle 参数 `-PciyingSigningProperties=完整路径` 指定私有配置。

私有配置需包含 `storeFile`、`storePassword`、`keyAlias`、`keyPassword`；storeFile 相对于配置文件目录解析。不要把真实配置提交或粘贴到 Issue。

在原 U 盘环境的项目根目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\build-release.ps1 -PciyingMirror=true
```

标准开发环境中配置好 JDK 17 与 Android SDK 后，可运行 `./gradlew :app:assembleRelease`（Windows 用 `gradlew.bat`）。没有签名配置时，release 任务会失败；debug 构建不需要正式密钥。

正式 APK 产物位于 `app/build/outputs/apk/release/app-release.apk`。签名验证可用 Android SDK Build Tools 的 `apksigner verify --verbose --print-certs`。应核对证书指纹与自己备份的公开证书一致；不要把 debug 证书混用为正式发布证书。

后续版本应使用同一密钥并递增 versionCode。首次从调试签名切换到正式签名通常需要卸载测试版，卸载会清除本机选词偏好。签名本身不改变功能、测试状态或机型兼容性。

密钥的备份与恢复说明随私人备份目录保存，不上传仓库。
