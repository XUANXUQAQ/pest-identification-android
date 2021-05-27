package com.rjgc.api;

import com.alibaba.fastjson.JSON;
import com.rjgc.utils.resp.ResBody;
import com.rjgc.utils.Base64Utils;
import com.rjgc.utils.resp.RespUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class LocalNetApi extends NanoHTTPD {

    private final File cachedDir;

    private final CnnApi cnnNet;

    private String predictFileName;

    private String photoBase64;

    public LocalNetApi(int port, File cachedDir, CnnApi net) {
        super(port);
        this.cachedDir = cachedDir;
        this.cnnNet = net;
    }


    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();
        if(RespUtils.isPreflightRequest(session)){
            // 如果是则发送CORS响应告诉浏览HTTP服务支持的METHOD及HEADERS和请求源
            return RespUtils.responseCORS(session);
        }
        if (Method.POST.equals(method)) {
            Map<String, Object> parameters;
            try {
                HashMap<String, String> tmp = new HashMap<>();
                session.parseBody(tmp);
                String info = tmp.get("postData");
                parameters = JSON.parseObject(info, Map.class);
                // upload photo
                if (uri.contains("uploadPhoto")) {
                    assert parameters != null;
                    String file = (String) parameters.get("file");
                    String name = (String) parameters.get("name");
                    predictFileName = name;
                    File photo = new File(cachedDir, name);
                    photoBase64 = file;
                    Base64Utils.base642Jpg(file, photo.getAbsolutePath());
                    return RespUtils.responseCORS(ResBody.success().toString(), session);
                } else if (uri.contains("startPredict")) {
                    String code = cnnNet.predict(new File(cachedDir, predictFileName).getAbsolutePath());
                    HashMap<String, Object> map = new HashMap<>();
                    HashMap<String, Object> infoMap = new HashMap<>();
                    HashMap<String, Object> codeMap = new HashMap<>();
                    codeMap.put(code, 1);
                    infoMap.put("statistics", codeMap);
                    infoMap.put("img", photoBase64);
                    map.put(predictFileName, infoMap);
                    return RespUtils.responseCORS(ResBody.success(map).toString(), session);
                }
            } catch (IOException | ResponseException e) {
                e.printStackTrace();
            }
        }
        return RespUtils.responseCORS(ResBody.error(40000, "无效的请求").toString(), session);
    }
}
