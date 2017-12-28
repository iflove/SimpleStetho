package com.roogle.simple.stetho.inspector.protocol;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.roogle.simple.stetho.common.DateUtil;
import com.roogle.simple.stetho.common.IParse;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class CommunicatingProtocol implements IParse<CommunicatingProtocol> {

    public static final String ACTION_QUERY_DATABASE = "query_database";
    public static final String ACTION_QUERY_LOGCAT = "query_logcat";
    public static final String ACTION_QUERY_LOGCAT_ANR = "query_logcat_anr";

    private static final String KEY_MESSAGE = "message";

    public String action;
    public Message message;

    @Override
    public CommunicatingProtocol parse(@NonNull String s) throws JSONException, IllegalArgumentException {
        JSONObject jsonObject = new JSONObject(s);
        this.action = jsonObject.optString("action");
        if (!jsonObject.has(KEY_MESSAGE)) {
            return this;
        }
        this.message = new Message();
        JSONObject message = jsonObject.getJSONObject(KEY_MESSAGE);
        this.message.tableName = message.optString("table_name");
        this.message.sql = message.optString("sql");
        this.message.returnDataType = message.optString("type");
        this.message.begin = message.optString("begin");
        this.message.end = message.optString("end");

        if (!TextUtils.isEmpty(this.message.begin) && !TextUtils.isEmpty(this.message.end)) {
            try {
                DateUtil.parse(this.message.begin, DateUtil.UTC_DATE_FORMAT_PATTERN);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Error: begin UTC-Time format wrong");
            }
            try {
                DateUtil.parse(this.message.end, DateUtil.UTC_DATE_FORMAT_PATTERN);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Error: begin UTC-Time format wrong");
            }
        }

        return this;
    }

    public class Message {
        public String tableName;
        public String sql;
        public String returnDataType;

        public String begin;
        public String end;
    }

}
