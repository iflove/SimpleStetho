package com.roogle.simple.stetho;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.roogle.simple.stetho.common.FunctionCallBack;
import com.roogle.simple.stetho.inspector.protocol.CommunicatingProtocol;

import org.json.JSONException;

public class SimpleStetho {

    private final Context context;
    private final DatabaseProvider databaseProvider;
    private final LogcatProvider logcatProvider;
    private String databaseName = null;

    public SimpleStetho(@NonNull final Context context) {
        this.context = context.getApplicationContext();
        this.databaseProvider = new DatabaseProvider(this.context);
        logcatProvider = new LogcatProvider(this.context);
    }

    public DatabaseProvider getDatabaseProvider() {
        return databaseProvider;
    }

    public LogcatProvider getLogcatProvider() {
        return logcatProvider;
    }

    public Context getContext() {
        return context;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void executeCommand(@NonNull String command, final FunctionCallBack<String> callBack) {
        if (callBack == null) {
            return;
        }
        try {
            CommunicatingProtocol protocol = new CommunicatingProtocol().parse(command);
            if (!TextUtils.isEmpty(protocol.action)) {
                switch (protocol.action) {
                    case CommunicatingProtocol.ACTION_QUERY_DATABASE: {
                        if (TextUtils.isEmpty(protocol.message.sql)) {
                            callBack.apply("Error: message sql parameters is Required ");
                            return;
                        }
                        if (this.databaseName == null) {
                            callBack.apply("Error: databaseName is Required Set");
                            return;
                        }
                        String result;
                        if ("json".equals(protocol.message.returnDataType)) {
                            result = getDatabaseProvider().getExecuteSQLResponseJson(this.databaseName, protocol.message.sql).toString();
                        } else {
                            result = getDatabaseProvider().getExecuteSQLResponseConsoleTableString(this.databaseName, protocol.message.sql);
                        }
                        callBack.apply(result);
                    }
                    break;
                    case CommunicatingProtocol.ACTION_QUERY_LOGCAT: {
                        if (checkQueryLogcatParameter(protocol)) {
                            getLogcatProvider().readLogcatLogFiles(protocol.message.begin,
                                    protocol.message.end, new FunctionCallBack<String>() {
                                        @Override
                                        public void apply(String s) {
                                            callBack.apply(s);
                                        }
                                    });
                        } else {
                            callBack.apply("Error: message begin end parameters is Required ");
                        }
                    }
                    break;
                    case CommunicatingProtocol.ACTION_QUERY_LOGCAT_ANR: {
                        getLogcatProvider().readTracesLogFile(new FunctionCallBack<String>() {
                            @Override
                            public void apply(String s) {
                                callBack.apply(s);
                            }
                        });
                    }
                    break;
                    default:
                        callBack.apply("Error: No support action");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof IllegalArgumentException) {
                callBack.apply(e.toString());
            } else if (e instanceof JSONException) {
                callBack.apply("Error: Command format wrong");
            }

        }
    }

    private boolean checkQueryLogcatParameter(CommunicatingProtocol serverSQLiteProtocol) {
        return serverSQLiteProtocol.message != null && serverSQLiteProtocol.message.begin != null
                && serverSQLiteProtocol.message.end != null;
    }
}
