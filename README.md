# UGlobalWeapon - Minecraft Plugin

专为 TensorPixel 的 UGlobal 生存设计的 Minecraft 武器插件，包含普通火箭炮与“寂灭”。服务端保持上游 Maven 项目结构；配套 Fabric 客户端提供模型、镜头、输入同步和黑洞透镜效果。

上游仓库：[qingchenyouforcc/UGlobalWeapon](https://github.com/qingchenyouforcc/UGlobalWeapon)。适用 Minecraft 1.21.1、Java 21；客户端使用 Fabric Loader 0.16.14 和 Fabric API。

## 目录

```text
UGlobalWeapon/
├── pom.xml                 Maven 服务端构建
├── src/                    服务端源码、资源与已有检查
├── client/                 Fabric 客户端（Gradle）
├── resource-pack/          Minecraft 资源包源码
├── models/                 Blockbench / Blender 工程
├── tools/                  构建、建模与开发工具源码
├── docs/                   开发说明、目录约定与历史
├── target/                 服务端产物（忽略）
├── dist/                   资源包 ZIP（忽略）
└── run/                    本地服务器、存档、缓存、备份（忽略）
```

详细说明：[目录约定](docs/PROJECT_STRUCTURE.md) · [构建与开发](docs/DEVELOPMENT.md) · [客户端](client/README.md)

## 构建

```powershell
mvn package
```

服务端产物：`target/weapon-1.0-SNAPSHOT.jar`。

```powershell
Push-Location client
gradle --gradle-user-home ../run/cache/gradle remapJar verifyClientJar
Pop-Location
./tools/build-resource-pack.ps1
```

客户端产物：`client/build/libs/uglobalweapon-client-1.4.0.jar`；资源包产物：`dist/UGlobalWeapon-Demo-ResourcePack.zip`。已有本地依赖时，可用 `tools/build-weapon.ps1` 仅编译打包服务端。

## 游戏内使用

- `/uglobalweapon rpg normal`：普通火箭炮。
- `/uglobalweapon 寂灭` 或 `/uglobalweapon annihilation`：寂灭。
- `/uglobalweapon ammo 16`：专用火箭弹。

寂灭需要匹配客户端 mod：按住右键蓄力，松手后灯槽逆序熄灭并取消，只有蓄满发射才扣弹。服务器负责弹药、吸引和爆炸；客户端负责按键同步及视觉表现。客户端 mod 不提供单人游戏武器逻辑。

## 本地运行数据

本地服务端位于 `run/paper-server/`，资源包托管目录为 `run/resource-pack-host/`。目录整理后两项后台服务均已停止；构建不会自动启动或部署服务。

世界、缓存、工具安装包及历史 JAR 已保留到 `run/` 或构建输出目录，并由 `.gitignore` 排除。版本演进记录在 [客户端历史](docs/CHANGELOG-client.md) 与 [服务端历史记录](docs/server-notes-history.md)。
