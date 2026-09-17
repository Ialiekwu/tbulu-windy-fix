# APK 签名与更新

本项目签名的是独立模块 `dev.local.windyfix`。两步路原版 APK 保持原有签名。

## 使用者

从 [GitHub Releases](https://github.com/Ialiekwu/tbulu-windy-fix/releases/latest) 下载正式 APK。
本项目发布的模块使用同一个签名密钥；后续版本可直接覆盖安装，正常情况下不需要卸载旧模块。

固定的签名证书 SHA-256：

```text
abf6ed8fe23f7e72df41caf2749a1ca160c3a22ef3e7273d2c6a36fbb6525122
```

证书指纹代表签名身份，跨版本保持不变。`SHA256SUMS` 中的 APK 文件哈希用来检查下载的文件，发布新版本时会变化。这两者不能互相代替。

安装时如果出现“签名不一致”，先核对 APK 来源和证书。不要为了解决这个提示去卸载两步路。其他人自行签名的模块，即使包名相同，也不能直接覆盖本项目的正式模块。

证书显示的名称 `CN=WindyFix Local Development` 是初次创建时的标签；APK 的更新身份由实际证书和密钥决定，不能为了更改显示名称而换一把密钥。

## 维护者

正式密钥不在仓库、源码压缩包、GitHub Release 或 Actions 中。默认本地位置：

```text
.tools/signing/windyfix.jks
.tools/signing/password
```

别名为 `windyfix`，RSA 3072 位。密码文件同时用于 keystore 和私钥密码。
上述文件必须与当前签名一致，不能使用新生成的密钥替代。

```sh
python3 scripts/bootstrap.py
bash scripts/test.sh
python3 scripts/check_signing.py
bash scripts/build.sh release
python3 scripts/verify_apk.py dist/release/windyfix-1.0.0.apk
```

正式构建在编译前读取已有证书并对比 `signing/certificate.sha256`；签名后再次验证 APK 的证书、包名、版本和 v2/v3 签名。任何检查失败都会退出，不会自动生成密钥。
APK 最低支持 Android 8.1（API 27），因此使用 v2/v3 签名；不需要 v1 签名。

可通过以下变量指定本地密钥位置，无需修改源码：

```sh
export WINDY_KEYSTORE=/your/private/path/windyfix.jks
export WINDY_KEYSTORE_PASSWORD_FILE=/your/private/path/password
export WINDY_KEY_ALIAS=windyfix
bash scripts/build.sh release
```

密钥目录建议权限 `700`、文件 `600`。将密钥和密码保存在可靠的私有备份中；同一硬盘上的副本不能防止硬盘损坏。`.private/` 已被 Git 忽略，可用于本机备份，但不要把备份上传到公开仓库。密钥丢失后，无法为现有安装签出同一身份的正常更新。

GitHub Actions 不接触正式密钥，只运行测试和构建未签名 APK。新贡献者执行 `bash scripts/build.sh unsigned` 即可验证构建，不需要向维护者索取密钥。

发布时使用递增的 `versionCode`，同步更新版本显示与发行说明；运行测试，签名并验签，将 APK、`SHA256SUMS`、`certificate.sha256` 和 `signature.txt` 上传到对应 Git tag 的 Release。
正式下载入口始终指向 Release 的已签名 APK。
