# UGlobalWeapon

Minecraft 1.21.1 武器插件，包含火箭炮与“寂灭”，配套 Fabric 客户端提供模型、输入同步和视觉效果。

基于 [qingchenyouforcc/UGlobalWeapon](https://github.com/qingchenyouforcc/UGlobalWeapon)。

## 项目结构

```text
├── pom.xml          服务端 Maven 构建配置
├── src/             服务端源码、插件配置与测试
├── client/          Fabric 客户端源码与 Gradle 构建配置
├── resource-pack/   游戏资源包：模型 JSON、贴图和声音配置
├── models/          可编辑的 Blockbench / Blender 模型
├── tools/           构建、建模与资源校验脚本
└── .gitignore       排除本地数据、缓存和构建产物
```

## 构建

需要 Java 21、Maven；客户端使用 Gradle 8.13，仓库未附带 Gradle Wrapper。

在仓库根目录执行：

```shell
mvn package
gradle -p client build
```

服务端 JAR 输出到 `target/`，客户端 JAR 输出到 `client/build/libs/`。
`resource-pack/` 可直接作为文件夹资源包使用。

## 使用

将服务端 JAR 放入 Minecraft 1.21.1 的 Paper 服务端 `plugins/` 目录。
客户端安装 Fabric Loader 0.16.14、Fabric API 0.116.5+1.21.1，并将客户端 JAR（非 sources JAR）放入 `mods/`。

- `/uglobalweapon rpg normal`：获取火箭炮。
- `/uglobalweapon annihilation`：获取寂灭。
- `/uglobalweapon ammo 16`：获取火箭弹。

命令需要 `uGlobalWeapon.UGlobalWeapon` 权限。客户端需配合服务端插件使用。

## 提交范围

提交源码、构建配置和资源源文件。`run/`、`target/`、`client/build/`、`dist/`、IDE 配置和缓存由 `.gitignore` 排除。
