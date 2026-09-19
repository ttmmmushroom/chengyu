# 好词好句 · APK 一键出包

把现有「好词好句」网页（PWA）整体打包成**可直接安装到安卓手机的 APK**。
原理：用 Android 原生 WebView（经 `WebViewAssetLoader` 把 `assets/` 映射为 https 源）加载网页，
因此网页里的拼音库、成语词典（`idiom.json` 的 `fetch`）、图标等都能离线正常加载，
GitHub Gist 云同步这类联网功能也照常可用。

仓库结构：

```
app/                    网页本体（index.html / pinyin-pro.js / idiom.json / 图标 / manifest）
android/                Android 工程（Gradle + WebView 外壳）
  app/src/main/assets/  构建时由 app/ 拷贝进来的网页资源（CI 自动完成）
  app/src/main/java/.../MainActivity.java   WebView 入口
.github/workflows/      build-apk.yml  GitHub Actions 一键出包
scripts/copy-assets.sh  本地构建时同步网页资源
```

## 一键出包（GitHub Actions）

1. 在 GitHub 新建一个仓库，把本目录全部内容 push 上去。
2. 进入仓库 **Actions → Build APK** 工作流，左侧 **Run workflow**（或只要 push 到 main/master 就自动跑）。
3. 构建完成后，在 Workflow 运行记录的 **Artifacts** 里下载 `goodwords-debug-apk`（里面是 `app-debug.apk`）。

> 调试版 APK 已自带调试签名，**无需任何密钥**即可安装。

## 安装到手机

- 把 `app-debug.apk` 传到手机，用文件管理器点击安装。
- 若提示「禁止安装未知来源应用」，按提示允许该来源即可（安卓 8+：设置 → 安全 → 安装未知应用）。
- 安装后桌面出现「好词好句」图标，打开即全屏卡片应用，与原网页体验一致。

## 更新网页内容后重新出包

改完 `app/` 下的网页，重新 push（或手动 Run workflow）即可，APK 会包含最新内容。

## 可选：生成签名发布版（release）APK

调试版足够日常使用；若要正式分发/上架，可生成签名 release：

1. 本地生成签名密钥（仅需一次）：
   ```bash
   keytool -genkeypair -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias goodwords
   ```
2. 把 `release-key.jks` 转成 base64：
   ```bash
   base64 -w0 release-key.jks > release-key.jks.b64
   cat release-key.jks.b64
   ```
3. 在 GitHub 仓库 **Settings → Secrets and variables → Actions** 添加：
   - `KEYSTORE_BASE64`：上面输出的 base64 文本
   - `KEYSTORE_PASSWORD`：密钥库密码
   - `KEY_ALIAS`：`goodwords`
   - `KEY_PASSWORD`：密钥密码
4. 手动 **Run workflow**，会额外产出 `goodwords-release-apk`（签名版）。

## 本地构建（可选）

需要 JDK 17 与 Android SDK（含 platform-34、build-tools;34.0.0）：

```bash
bash scripts/copy-assets.sh
cd android
./gradlew assembleDebug      # 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 说明

- 应用包名：`com.goodwords.app`，最低支持 Android 5.0（API 21）。
- 离线可用：所有网页资源都打进 APK；仅云同步等功能需要联网。
- WebView 内返回键会先回退网页历史，再退出应用；沉浸式全屏。
