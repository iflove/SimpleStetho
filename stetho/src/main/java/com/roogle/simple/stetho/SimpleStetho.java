package com.roogle.simple.stetho;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.roogle.simple.stetho.common.Consumer;

import org.json.JSONException;
import org.json.JSONObject;

public class SimpleStetho {

    public static final String ACTION_QUERY_DATABASE = "query_database";
    public static final String ACTION_QUERY_LOGCAT = "query_logcat";
    public static final String ACTION_QUERY_LOGCAT_ANR = "query_logcat_anr";

    private final Context context;
    private final DatabaseProvider databaseProvider;
    private final LogcatProvider logcatProvider;
    private final SysLogcatProvider sysLogcatProvider;
    private String databaseName = null;

    public SimpleStetho(@NonNull final Context context) {
        this.context = context.getApplicationContext();
        this.databaseProvider = new DatabaseProvider(this.context);
        logcatProvider = new LogcatProvider(this.context);
        sysLogcatProvider = new SysLogcatProvider(this.context);
    }

    public DatabaseProvider getDatabaseProvider() {
        return databaseProvider;
    }

    public LogcatProvider getLogcatProvider() {
        return logcatProvider;
    }

    public SysLogcatProvider getSysLogcatProvider() {
        return sysLogcatProvider;
    }

    public Context getContext() {
        return context;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void executeCommand(@NonNull final String command, final Consumer<String> callBack) {
        if (callBack == null) {
            return;
        }
        try {
            JSONObject commandJsonObject = new JSONObject(command);
            final String action = commandJsonObject.optString("action");
            final JSONObject messageJsonObject = commandJsonObject.optJSONObject("message");

            if (!TextUtils.isEmpty(action)) {
                switch (action) {
                    case ACTION_QUERY_DATABASE: {
                        if (this.databaseName == null) {
                            throw new IllegalArgumentException("Error: Default DatabaseName is Required Set");
                        }
                        if (messageJsonObject == null) {
                            throw new IllegalArgumentException("Error: message parameters is Required ");
                        }
                        String sql = messageJsonObject.optString("sql");
                        if (TextUtils.isEmpty(sql)) {
                            throw new IllegalArgumentException("Error: message sql parameters is Required ");
                        }
                        String result = getDatabaseProvider().getExecuteSQLResponseTableText(this.databaseName, sql);
                        callBack.apply(result);
                    }
                    break;
                    case ACTION_QUERY_LOGCAT: {
                        if (messageJsonObject == null) {
                            throw new IllegalArgumentException("Error: message parameters is Required ");
                        }
                        String begin = messageJsonObject.optString("begin");
                        String end = messageJsonObject.optString("end");
                        if (!TextUtils.isEmpty(begin) && !TextUtils.isEmpty(end)) {
                            getLogcatProvider().readLogcatLogFiles(begin, end, new Consumer<String>() {
                                @Override
                                public void apply(String s) {
                                    callBack.apply(s);
                                }
                            });
                        } else {
                            throw new IllegalArgumentException("Error: message begin end parameters is Required ");
                        }
                    }
                    break;
                    case ACTION_QUERY_LOGCAT_ANR: {
                        getLogcatProvider().readTracesLogFile(new Consumer<String>() {
                            @Override
                            public void apply(String s) {
                                callBack.apply(s);
                            }
                        });
                    }
                    break;
                    default:
                        throw new IllegalArgumentException("Error: No support action");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof IllegalArgumentException) {
                callBack.apply(e.getMessage());
            } else if (e instanceof JSONException) {
                callBack.apply("Error: Command format wrong");
            }
        }
    }

}
