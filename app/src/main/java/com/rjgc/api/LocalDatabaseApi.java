package com.rjgc.api;

import android.content.Context;

import com.getcapacitor.ui.Toast;
import com.rjgc.utils.resp.ResBody;
import com.rjgc.utils.resp.RespUtils;
import com.rjgc.sqlite.Species;
import com.rjgc.sqlite.SqliteUtils;

import java.util.HashMap;
import java.util.LinkedList;

import fi.iki.elonen.NanoHTTPD;

/**
 * 数据库api本地模拟
 */
public class LocalDatabaseApi extends NanoHTTPD {

    private final SqliteUtils sqliteUtils;
    private final Context context;

    public LocalDatabaseApi(int port, Context context, String remoteApiUrl) {
        super(port);
        this.sqliteUtils = SqliteUtils.getInstance(context, remoteApiUrl);
        this.context = context;
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
                Toast.show(context, "当前无网络，识别功能受限");
                return RespUtils.responseCORS(ResBody.success(map).toString(), session);
            }
        }
        return RespUtils.responseCORS(ResBody.error(40000, "无效的请求").toString(), session);
    }
}
