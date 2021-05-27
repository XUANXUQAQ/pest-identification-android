package com.rjgc.utils.resp;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class RespUtils {

    /**
     * 封装响应包
     *
     * @param session http请求
     * @param resp    响应包
     * @return resp
     */
    private static NanoHTTPD.Response wrapResponse(NanoHTTPD.IHTTPSession session, NanoHTTPD.Response resp) {
        if (null != resp) {
            Map<String, String> headers = session.getHeaders();
            resp.addHeader("Access-Control-Allow-Credentials", "true");
            // nanohttd将所有请求头的名称强制转为了小写
            String origin = "*";
            resp.addHeader("Access-Control-Allow-Origin", origin);

            String requestHeaders = headers.get("access-control-request-headers");
            if (requestHeaders != null) {
                resp.addHeader("Access-Control-Allow-Headers", requestHeaders);
            }
        }
        return resp;
    }

    /**
     * 判断是否为CORS 预检请求请求(Preflight)
     *
     * @param session session
     * @return resp
     */
    public static boolean isPreflightRequest(NanoHTTPD.IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        return NanoHTTPD.Method.OPTIONS.equals(session.getMethod())
                && headers.containsKey("origin")
                && headers.containsKey("access-control-request-method")
                && headers.containsKey("access-control-request-headers");
    }

    /**
     * 向响应包中添加CORS包头数据
     *
     * @param session session
     * @return resp
     */
    public static NanoHTTPD.Response responseCORS(NanoHTTPD.IHTTPSession session) {
        NanoHTTPD.Response resp = wrapResponse(session, newFixedLengthResponse(""));
        resp.addHeader("Access-Control-Allow-Methods", "POST,GET,OPTIONS");
        String allowHeaders = "Content-Type";
        resp.addHeader("Access-Control-Allow-Headers", allowHeaders);
        resp.addHeader("Access-Control-Max-Age", "0");
        return resp;
    }

    /**
     * 向响应包中添加CORS包头数据
     *
     * @param session session
     * @return resp
     */
    public static NanoHTTPD.Response responseCORS(String msg, NanoHTTPD.IHTTPSession session) {
        NanoHTTPD.Response resp = wrapResponse(session, newFixedLengthResponse(msg));
        resp.addHeader("Access-Control-Allow-Methods", "POST,GET,OPTIONS");
        String allowHeaders = "Content-Type";
        resp.addHeader("Access-Control-Allow-Headers", allowHeaders);
        resp.addHeader("Access-Control-Max-Age", "0");
        return resp;
    }
}
