package com.youkeda.comment.exception;

/**
 * 敏感词命中异常
 *
 * 为什么用异常而不是返回码：
 *   Service 层的 insert 方法返回值是 int（受影响行数），
 *   没法同时表达"插入失败的原因"。抛异常可以让调用方（Controller）
 *   决定怎么呈现给用户，Service 不需要关心 HTTP 怎么返回。
 *
 * 继承 RuntimeException 而不是 Exception：
 *   业务校验失败属于运行时问题，不需要调用方强制 try-catch，
 *   由统一的 Controller 处理即可。
 */
public class SensitiveWordException extends RuntimeException {

    public SensitiveWordException(String message) {
        super(message);
    }
}
