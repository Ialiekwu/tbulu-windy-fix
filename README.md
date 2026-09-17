# 两步路 Windy 修复

[![Build and test](https://github.com/Ialiekwu/tbulu-windy-fix/actions/workflows/ci.yml/badge.svg)](https://github.com/Ialiekwu/tbulu-windy-fix/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/Ialiekwu/tbulu-windy-fix)](https://github.com/Ialiekwu/tbulu-windy-fix/releases/latest)

为两步路户外助手的 Windy 地图瓦片请求补全 `Referer: https://www.windy.com/` 的 LSPosed 模块。已在真机恢复 Windy 图层请求，并完成两步路原生离线下载测试。

**[下载正式 APK](https://github.com/Ialiekwu/tbulu-windy-fix/releases/latest)** · [签名说明](docs/signing.md) · [验证记录](docs/testing.md) · [反馈问题](https://github.com/Ialiekwu/tbulu-windy-fix/issues)

## 安装使用

需要一台已 root、**LSPosed 正常工作**的手机，以及两步路户外助手。KernelSU 提供 root；还需要单独配置并启用适合自己设备的 LSPosed 环境。只有安装 KernelSU 并不能运行本模块。

1. 从上面的 Release 页面下载并安装 `windyfix-版本号.apk`。
2. 在 LSPosed → 模块中启用「两步路 Windy 修复」，作用域只勾选「两步路户外助手」。
3. 在系统设置中**强制停止两步路**，然后重新打开，让主进程和下载进程重新加载模块。必要时重启手机。
4. 选择 Windy 图层，正常浏览或框选区域下载离线地图；原有失败任务可尝试点「继续」。

切换图层后暂时空白，可以轻轻拖动或缩放一次刷新。已装过本项目同签名模块的用户可直接覆盖安装更新。

停用时，在 LSPosed 中关闭模块，再强制停止并重开两步路；也可卸载本模块。已下载的地图由两步路管理。

## 图源设置

已有 Windy 图源时直接选择即可，不用重新添加。本模块处理网络请求头，**不会自动创建图源，也未修改两步路的图源名称或 URL 校验**。

没有图源的用户，如果所用两步路版本支持自定义 XYZ 图源，可尝试下列配置：

| 设置项 | 内容 |
| --- | --- |
| 名称 | 户外地形 |
| URL | `https://tiles.windy.com/v1/maptiles/outdoor/256%402x/{$z}/{$x}/{$y}/?lang=en` |
| 坐标系（如有） | GPS / WGS84 |
| 瓦片类型（如有） | XYZ |
| 图片尺寸（如有） | 512×512，高分辨率瓦片 |
| 级别（如有） | 1–17 |

URL 中的 `{$z}`、`{$x}`、`{$y}` 是两步路图源变量，需按原样保留。不同版本的设置界面可能不同；若名称包含 `windy` 时无法保存，可尝试表中的名称。若 URL 本身被拒绝，当前模块不处理该限制。上述真机下载测试使用已存在的图源，未对各版本的新建图源流程做完整验证。

## 兼容性

| 项目 | 已测试版本 |
| --- | --- |
| Android | 16 / API 36，arm64 |
| Root | KernelSU |
| LSPosed | 2.1.1 (7790) |
| 两步路户外助手 | 9.2.3 (1620)，`com.lolaage.tbulu.tools` |
| 模块 | 1.0.0，`dev.local.windyfix` |

APK 的最低 Android 版本为 8.1（API 27），使用 Xposed API 82。其他 Android、LSPosed、两步路版本尚未完成真机验证。服务端策略或应用更新可能改变兼容性。

实测通过两步路下载 4 张新瓦片，4 次请求均为 `HTTP 200 image/png`，原生任务状态为 `Finished`，4 张 512×512 PNG 校验通过；详见[验证记录](docs/testing.md)。

## APK 签名

这是单独安装的模块，**不会重新签名两步路**。正式模块沿用同一把密钥，支持后续版本覆盖安装。

固定签名证书 SHA-256：

```text
abf6ed8fe23f7e72df41caf2749a1ca160c3a22ef3e7273d2c6a36fbb6525122
```

每个 Release 附带 APK 文件哈希 `SHA256SUMS` 和验签结果 `signature.txt`。文件哈希随版本变化；证书指纹代表固定签名身份。私钥和密码不会发布到 GitHub。

**请安装 Release 中的正式 APK。** Actions 里的 `unsigned` 文件供开发者检查构建，不能直接安装。[签名与维护者备份说明 →](docs/signing.md)

## LSPatch 是什么？

朋友提到的“单应用 hook 可以 LSPatch 集成”，意思是将 hook 运行环境和模块嵌入被修改的目标 APK，尝试让它在没有系统级 LSPosed 的环境中运行。

| 方式 | 本项目当前采用的 LSPosed 模块 | LSPatch 集成 |
| --- | --- | --- |
| Root | 需要 root 和可用的 LSPosed | 设计上可不依赖 root |
| 两步路 APK | 使用原版 | 需要修改并重新签名 |
| 两步路签名 | 保留官方签名 | 变为补丁包签名，影响覆盖安装和官方更新 |
| 兼容性 | 上表环境已经验证 | 未验证当前两步路及 Android 16 |

[原版 LSPatch 仓库](https://github.com/LSPosed/LSPatch)已归档，官方最后发布版本为 v0.6（2023-11-23，截至 2026-09-18 核查）。它不能直接视为当前模块的可靠免 root 安装方式。本项目首版提供已经实测的 LSPosed 模块，不分发重新打包的两步路 APK。

## 常见问题

**安装模块后还是下载失败？**

确认 LSPosed 正在运行、模块已启用、作用域包含两步路，并强制停止两步路后重开。在 LSPosed 日志中搜索 `[WindyFix]`；正常情况下能看到 `loaded`、`OkHttp newCall hook installed`，请求时会出现 `Patched Windy request`。响应诊断可用时会记录 HTTP 状态和内容类型。

**日志有补全请求，但仍然是 403？**

补全 Referer 只能解决已经验证的这一类问题。服务端规则、限流、网络环境或图源 URL 变化仍可能造成失败。提交问题时附上版本和少量 `[WindyFix]` 日志即可。

**显示空白，但下载任务已经完成？**

确认选中的是下载时使用的图层，并位于已下载区域及级别内；拖动或缩放刷新一次。出行前可临时断网，检查目标区域的显示情况。

**安装提示签名冲突？**

先核对下载来源和上面的证书指纹。自行编译并用其他密钥签名的同包名模块不能直接覆盖正式版本。无需为此卸载两步路。

## 实现范围

只在 `com.lolaage.tbulu.tools` 中加载，并且只匹配 HTTPS、域名严格等于 `tiles.windy.com`、默认端口或 443、无 URL 用户信息、路径以 `/v1/maptiles/` 开头的请求。

主要 hook 两步路自身的 `okhttp3.OkHttpClient.newCall(Request)`：复制请求并设置 Referer，保留 URL、请求方法、请求体、标签和其他请求头。另对 `URL.openConnection` 的对应连接设置请求头，作为兼容路径；该路径没有独立完成真机下载验证。模块不申请 Android 权限，不修改 TLS 校验、代理或地图数据库。

日志只记录加载信息、补全计数、HTTP 状态、内容类型和异常类型，不输出完整 URL、坐标、账号或完整请求头。已知限制是依赖两步路保留相关 OkHttp 类和方法名称；HttpURLConnection 设置的请求头也可能被调用方后续覆盖。

## 从源码构建

需要 Linux x86_64、JDK 17、Python 3.10+、Bash，以及首次下载依赖的网络连接。无需 Gradle 或 Android Studio。

```sh
git clone https://github.com/Ialiekwu/tbulu-windy-fix.git
cd tbulu-windy-fix
python3 scripts/bootstrap.py
bash scripts/test.sh
bash scripts/build.sh unsigned
```

输出为 `dist/unsigned/windyfix-1.0.0-unsigned.apk`。依赖 URL 和 SHA-256 固定在 `dependencies.lock.json`。使用 Android Build Tools 35、Android 35 API、Xposed API 82；Xposed API 和测试依赖不会被打包到 APK 中。

维护者持有原签名密钥时可运行 `bash scripts/build.sh release`。构建会核对固定证书，密钥缺失或不同均会失败，不会自动生成替代密钥。详见[签名文档](docs/signing.md)。

## 许可

模块源代码采用 [MIT License](LICENSE)。这是独立的非官方项目，与两步路、Windy 无隶属关系；模块源码许可不包含第三方地图数据的授权，使用图源时请遵循其服务条款。
