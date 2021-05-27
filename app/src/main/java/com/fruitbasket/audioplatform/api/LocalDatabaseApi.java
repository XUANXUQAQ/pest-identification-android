package com.fruitbasket.audioplatform.api;

import android.content.Context;

import com.fruitbasket.audioplatform.resp.ResBody;
import com.fruitbasket.audioplatform.utils.RespUtils;
import com.fruitbasket.audioplatform.utils.sqlite.Species;
import com.fruitbasket.audioplatform.utils.sqlite.SqliteUtils;

import java.util.HashMap;
import java.util.LinkedList;

import fi.iki.elonen.NanoHTTPD;

public class LocalDatabaseApi extends NanoHTTPD {

    private final SqliteUtils sqliteUtils;

    public LocalDatabaseApi(int port, Context context, String remoteApiUrl) {
        super(port);
        this.sqliteUtils = SqliteUtils.getInstance(context, remoteApiUrl);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();
        if (Method.GET.equals(method)) {
            if (uri.contains("species")) {
                String[] split = uri.split("/");
                String code = split[split.length - 1];
                Species species = sqliteUtils.selectSpeciesByCode(code);
                HashMap<String, Object> map = new HashMap<>();
                LinkedList<Species> list = new LinkedList<>();
                list.add(species);
                map.put("pages", 1);
                map.put("data", list);
                return RespUtils.responseCORS(ResBody.success(map).toString(), session);
            }
        }
        return RespUtils.responseCORS(ResBody.error(40000, "无效的请求").toString(), session);
    }
}
