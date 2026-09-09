# 开发与构建

所有命令以仓库根目录为起点。Java 21、Minecraft 1.21.1、Fabric Loader 0.16.14；客户端当前版本 1.4.0。

## 服务端

标准 Maven 构建保持上游入口：

```powershell
mvn package
```

输出为 `target/weapon-1.0-SNAPSHOT.jar`。本地已有依赖时可使用只编译打包的离线脚本：

```powershell
./tools/build-weapon.ps1
```

该脚本使用 `JAVA_HOME`，也支持 `-JavaHome`；依赖位于 `run/dependencies/spigot-api-1.21.1.jar` 和 `run/paper-server/libraries/`。这些本地文件不进入 Git。新克隆优先使用 Maven 下载依赖。

## 客户端

```powershell
Push-Location client
gradle --gradle-user-home ../run/cache/gradle remapJar verifyClientJar
Pop-Location
```

输出为 `client/build/libs/uglobalweapon-client-1.4.0.jar`。已下载依赖可增加 `--offline`。没有附带 Gradle 安装包或 Wrapper，需要本机 Gradle 8.13。

客户端使用 Fabric API 0.116.5+1.21.1。若 `run/dependencies/fabric-api/` 已有本机缓存的 networking/base 模块，构建可离线复用；否则从公开 Maven 仓库解析完整 Fabric API。游戏内仍需安装 Fabric API。

## 模型和资源包

```powershell
python tools/modeling/build_annihilation.py
./tools/build-resource-pack.ps1
```

Python 生成器同步普通火箭炮、寂灭和各动画帧。资源包输出至 `dist/UGlobalWeapon-Demo-ResourcePack.zip`；若本地 `run/resource-pack-host/` 和 Paper 配置存在，脚本同步托管副本并更新 SHA-1。脚本不会启动服务器。

可用 `-Python` 或 `WEAPON_PYTHON` 指定 Python。已有 Minecraft 缓存时同时验证原版物品图集；新克隆没有缓存时仍会验证 JSON、PNG 和资源引用，并明确跳过图集验证。

Blender 后台入口为 `tools/modeling/render_models.py` 和 `tools/modeling/render_annihilation.py`；可编辑文件保存在 `models/`。预览 PNG、Blender 自动备份和中间几何 JSON 被忽略。

## 本地服务

现有服务端与存档位于 `run/paper-server/`，资源包 HTTP 根目录为 `run/resource-pack-host/`。本次整理后两项服务均已停止。

需要重新启动时，可手动运行 `run/paper-server/start.bat`。现有启动脚本使用本机 Java/Python 路径；换机器时按实际安装位置修改。空环境可显式运行 `tools/setup-paper.ps1` 下载并配置本地 Paper；该脚本只配置，不自动启动。

资源包地址仍为本机 `127.0.0.1:8765`，游戏端口仍为 25565。世界、配置、下载缓存、日志和历史 JAR 均保留在忽略目录，不应加入 Git。

## 检查与历史

目录移动后验证源码和资源哈希、构建路径与编译即可；无需启动游戏或破坏测试世界。

`tools/test-spread.ps1`、`tools/effects-smoke/` 和 `tools/modeling/LensShaderPreview.java` 保留为显式调用的开发检查入口。历史版本说明在 `CHANGELOG-client.md` 和 `server-notes-history.md`，其中旧目录和旧参数不代表当前版本。
