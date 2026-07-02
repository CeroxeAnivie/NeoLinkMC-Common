# NeoLinkMC Common

NeoLinkMC Common 是 NeoLinkMC 的公共核心库，不是玩家直接安装的 Mod。

本仓库负责提供跨 Fabric、Forge、NeoForge 共享的配置模型、连接参数校验、UUID 修正、消息转发与 NeoLink 隧道生命周期管理。所有加载器适配仓库都应依赖这里的统一实现，避免不同平台之间出现端口范围、默认值、停止逻辑或聊天提示不一致的问题。

## 模块声明

- 模块名称：NeoLinkMC Common
- 模块类型：公共 JVM 核心库
- Java 目标版本：17
- 发布坐标：`top.ceroxe.neolinkmc:neolinkmc-common`
- 主要用途：供 NeoLinkMC Fabric、Forge、NeoForge 版本打包内置或开发期引用

## 功能范围

- 统一读取和迁移 `config/neolinkmc/config.json`
- 统一校验远程地址、端口、玩家数、游戏模式和正版/离线模式
- 管理 NeoLinkAPI 客户端启动、停止、状态同步与错误提示
- 处理低流量提醒、连接日志和 Minecraft 聊天消息转发
- 为不同 Minecraft 加载器提供稳定的公共行为边界

## 构建与验证

优先使用仓库自带 Gradle Wrapper：

```cmd
chcp 65001 >nul
gradlew.bat test
```

供本地加载器仓库引用时，先发布到仓库内开发 Maven 目录：

```cmd
chcp 65001 >nul
gradlew.bat publishLocalDevelopmentPublicationToLocalDevelopmentRepository
```

正式发布仍应走完整 `publish` / signing 流程。本地开发发布目录只用于 NeoLinkMC 加载器仓库的构建验证，不应当作为正式制品源。
