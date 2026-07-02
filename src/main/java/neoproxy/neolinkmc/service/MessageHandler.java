package neoproxy.neolinkmc.service;

import org.jetbrains.annotations.NotNull;

/**
 * 消息处理器接口 - 简化版本
 * <p>
 * 设计原则：
 * 1. 接口隔离 - 只定义消息处理相关方法
 * 2. 依赖倒置 - ConnectionService 依赖抽象而非具体实现
 * 3. 单一职责 - 只负责消息的展示和日志记录
 *
 * @author NeoProxy Team
 * @version 0.3.1
 */
public interface MessageHandler {

    /**
     * 发送给玩家看的精简消息。
     *
     * <p>实现不得在这里顺手写控制台日志；控制台日志必须由调用方显式调用
     * {@link #log(String, LogLevel)} 或 {@link #log(String, LogLevel, Throwable)}。
     * 这样聊天栏可以保持极简，而控制台保留完整 debug 信息。</p>
     *
     * @param message 消息内容
     * @param type    消息类型：info(蓝色)、success(绿色)、warning(黄色)、error(红色)
     */
    void send(@NotNull String message, @NotNull MessageType type);

    /**
     * 记录日志
     *
     * @param message 日志内容
     * @param level   日志级别
     */
    void log(@NotNull String message, @NotNull LogLevel level);

    /**
     * 记录日志（带异常）
     *
     * @param message   日志内容
     * @param level     日志级别
     * @param throwable 异常对象
     */
    void log(@NotNull String message, @NotNull LogLevel level, @NotNull Throwable throwable);

    /**
     * 消息类型枚举
     */
    enum MessageType {
        INFO, SUCCESS, WARNING, ERROR
    }

    /**
     * 日志级别枚举
     */
    enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}
