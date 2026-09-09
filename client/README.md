# UGlobalWeapon Fabric 客户端

当前版本 **1.4.0**，面向 Minecraft 1.21.1、Java 21、Fabric Loader 0.16.14，游戏中需安装 Fabric API。配合仓库根目录的 Paper 插件使用。

## 功能

- 火箭炮模型、发热渐变和无附魔闪光渲染。
- 寂灭按住蓄力/松手退灯的输入同步。
- 镜头震动、顿帧、黑白及白光效果。
- 基于科学参考的屏幕空间黑洞透镜、吸积盘、光子环与频移近似。

弹药、武器伤害、地形破坏由服务端处理。客户端通过 `uglobalweapon:charge` 发送按键状态，无法独立提供单人游戏武器逻辑。

## 构建与安装

在本目录运行：

```powershell
gradle --gradle-user-home ../run/cache/gradle remapJar verifyClientJar
```

将 `build/libs/uglobalweapon-client-1.4.0.jar` 安装到目标游戏实例的 `mods/`，与 Fabric API 一起使用；不要安装 sources JAR。完全重启游戏后生效。

资源从同仓库的 `../resource-pack/` 打包，避免重复维护模型。更多构建说明见 [开发文档](../docs/DEVELOPMENT.md)，历史改动见 [版本记录](../docs/CHANGELOG-client.md)。
