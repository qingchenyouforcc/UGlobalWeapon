# 项目目录约定

上游：[qingchenyouforcc/UGlobalWeapon](https://github.com/qingchenyouforcc/UGlobalWeapon)。整理时通过 `git ls-remote --symref origin HEAD` 确认默认分支为 `master`，远端 HEAD 与本地基线均为 `3f8b2bbc92031265a805fc889a5239c93425c65d`。

上游没有额外的 CONTRIBUTING、AGENTS 或多模块目录规范，已有结构是 Maven 根目录的 `pom.xml`、`src/main/java` 和 `src/main/resources`。本次沿用这些约定，保留原来的 Git 历史和 Java 包路径，将本地新增的配套项目纳入同一仓库。

| 目录 | 用途 | Git |
| --- | --- | --- |
| `src/main/java/org/mmga/uglobal/` | 服务端插件实现 | 跟踪 |
| `src/main/resources/` | Bukkit 插件元数据 | 跟踪 |
| `src/test/java/` | 服务端已有回归检查 | 跟踪 |
| `client/` | 独立 Gradle/Fabric 客户端项目 | 跟踪源码 |
| `resource-pack/` | 资源包的 `pack.mcmeta` 与 `assets/` | 跟踪 |
| `models/` | Blockbench、Blender 可编辑工程 | 跟踪源文件 |
| `tools/` | 构建、资源生成、建模与检查脚本 | 跟踪源码 |
| `docs/` | 开发说明、历史记录 | 跟踪 |
| `target/`、`client/build/` | Maven/Gradle 构建结果 | 忽略 |
| `dist/` | 可分发资源包 ZIP | 忽略 |
| `run/paper-server/` | 本地 Paper 服务端、世界、日志、配置 | 忽略 |
| `run/resource-pack-host/` | 本地 HTTP 服务使用的资源包副本 | 忽略 |
| `run/cache/gradle/` | Gradle/Minecraft 本地缓存 | 忽略 |
| `run/toolchains/` | 下载的 Maven 等工具 | 忽略 |
| `run/dependencies/` | 离线构建所用 API JAR | 忽略 |
| `run/backups/` | 历次 mod/插件备份 | 忽略 |
| `run/validation/` | 已有隔离检查服务器及世界 | 忽略 |
| `run/reorganization/` | 本次移动清单和源文件校验清单 | 忽略 |

客户端没有成为 Maven 子模块；服务端继续使用原来的 Maven 构建入口。客户端通过 `../resource-pack` 引用同一份模型资源，避免维护重复的资源包源码。

目录整理不自动启动或部署服务器。当前服务已停止；需要启动时手动执行开发说明中的命令。
