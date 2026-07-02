# NeoLinkMC-Common

NeoLinkMC-Common 是 NeoLinkMC 的公共 JVM 内核仓库，提供配置模型、平台无关的连接服务、公共状态机和共享工具类。

## 发布坐标

```text
groupId    = top.ceroxe.neolinkmc
artifactId = neolinkmc-common
version    = 0.3.1
```

## 目标

- 不直接耦合 Fabric / Forge / NeoForge 入口
- 作为 `NeoLinkMC-Fabric-NeoForge` 与 `NeoLinkMC-Forge` 的共享依赖
- 通过 Maven 制品复用，而不是复制源码

## 本地验证

```cmd
gradlew.bat test
gradlew.bat publishToMavenLocal
```
