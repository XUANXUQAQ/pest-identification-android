package com.rjgc.api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.alibaba.fastjson.JSON;
import com.rjgc.utils.Base64Utils;
import com.rjgc.utils.NetUtils;
import com.rjgc.utils.resp.ResBody;
import com.rjgc.utils.resp.RespUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * 神经网络api本地模拟
 */
public class LocalNetApi extends NanoHTTPD {

    private final File cachedDir;

    private String predictFileName;

    public LocalNetApi(int port, File cachedDir) {
        super(port);
        this.cachedDir = cachedDir;
    }


    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();
        if (RespUtils.isPreflightRequest(session)) {
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
                    Base64Utils.base642Jpg(file, photo.getAbsolutePath());
                    return RespUtils.responseCORS(ResBody.success().toString(), session);
                } else if (uri.contains("startPredict")) {
                    Bitmap bitmap;
                    File path = new File(cachedDir, predictFileName);
                    try (FileInputStream fis = new FileInputStream(path)) {
                        bitmap = BitmapFactory.decodeStream(fis);
                        bitmap = zoomImg(bitmap, 640, 640);
                        NetUtils.INSTANCE.send(bitmap);
                        Object[] predictInfo = NetUtils.INSTANCE.getResult(bitmap);

                        HashMap<String, Integer> codeMap = (HashMap<String, Integer>) predictInfo[0];

                        HashMap<String, Object> map = new HashMap<>();
                        HashMap<String, Object> resultWrapper = new HashMap<>();

                        resultWrapper.put("statistics", codeMap);
                        Bitmap processedImg = (Bitmap) predictInfo[1];
                        resultWrapper.put("img", Base64Utils.bitmap2Base64(processedImg));
                        map.put(predictFileName, resultWrapper);
                        return RespUtils.responseCORS(ResBody.success(map).toString(), session);
                    }
                }
            } catch (IOException | ResponseException e) {
                e.printStackTrace();
            }
        }
        return RespUtils.responseCORS(ResBody.error(40000, "无效的请求").toString(), session);
    }

    @SuppressWarnings("SameParameterValue")
    private Bitmap zoomImg(Bitmap bm, int newWidth, int newHeight) {
        return Bitmap.createScaledBitmap(bm, newWidth,newHeight, true);
    }
}
