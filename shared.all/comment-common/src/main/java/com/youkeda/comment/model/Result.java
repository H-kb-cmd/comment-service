package com.youkeda.comment.model;

/**
 * 统一返回包装（评论服务自己的版本）
 *
 * 原本由私有 starter 提供，此处补齐。
 * 依据 CommentServiceImpl 的实际调用反推，需要四个方法：
 *   result.setCode("500");
 *   result.setMessage("refId、userId、content 不能为空");
 *   result.setData(commentDO.toModel());
 *   result.setSuccess(true);
 *
 * 注意：code 是 String 类型（不是 int），这是这个项目的历史约定。
 *      第 4 周 Day 2 会统一成一套规范。
 */
public class Result<T> {

    /** 状态码，成功为 "200" */
    private String code = "200";

    /** 提示信息 */
    private String message = "success";

    /** 是否成功 —— 给前端一个明确的布尔判断，比解析 code 更直接 */
    private boolean success;

    /** 业务数据 */
    private T data;

    public Result() {
    }

    public Result(T data) {
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static <T> Result<T> ok(T data){
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }
    public static <T> Result<T> fail(String message){
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode("500");
        result.setMessage(message);
        return result;
    }
}

