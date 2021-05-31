package com.rjgc.utils.resp;

import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.Map;

public class ResBody<T> {
    /**
     * 响应代码
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应结果
     */
    private Object result;

    private static final Map<String, Object> RESULT_HOLDER = new HashMap<>();

    static {
        RESULT_HOLDER.put("pages", 1);
        RESULT_HOLDER.put("data", "");
    }

    /**
     * 成功
     *
     * @return resBody
     */
    public static <T> ResBody<T> success() {
        return success(null);
    }

    /**
     * 成功
     *
     * @param data data
     * @return resBody
     */
    public static <T> ResBody<T> success(T data) {
        ResBody<T> rb = new ResBody<>();
        rb.setCode(20000);
        rb.setMessage("成功");
        rb.setResult(data);
        return rb;
    }


    public static <T> ResBody<T> error(int errorCode, String errorInfo) {
        ResBody<T> rb = new ResBody<>();
        rb.setCode(errorCode);
        rb.setMessage(errorInfo);
        rb.setResult(RESULT_HOLDER);
        return rb;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    private void setResult(Object result) {
        this.result = result;
    }

    private void setMessage(String message) {
        this.message = message;
    }

    private void setCode(int code) {
        this.code = code;
    }

    public Object getResult() {
        return result;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }
}
