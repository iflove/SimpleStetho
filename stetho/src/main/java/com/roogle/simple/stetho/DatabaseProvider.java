/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.roogle.simple.stetho;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.lazy.library.logging.Logcat;
import com.roogle.simple.stetho.common.ConsoleTable;
import com.roogle.simple.stetho.inspector.database.DefaultDatabaseConnectionProvider;
import com.roogle.simple.stetho.inspector.database.DefaultDatabaseFilesProvider;
import com.roogle.simple.stetho.inspector.database.SqliteDatabaseDriver;
import com.roogle.simple.stetho.inspector.protocol.module.BaseDatabaseDriver;
import com.roogle.simple.stetho.inspector.protocol.module.DatabaseDescriptor;
import com.roogle.simple.stetho.inspector.protocol.module.DatabaseDriver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DatabaseProvider {
    /**
     * The protocol doesn't offer an efficient means of pagination or anything like that so
     * we'll just cap the result list to some arbitrarily large number that I think folks will
     * actually need in practice.
     * <p>
     * Note that when this limit is exceeded, a dummy row will be introduced that indicates
     * truncation occurred.
     */
    private static final int MAX_EXECUTE_RESULTS = 250;

    /**
     * Maximum length of a BLOB field before we stop trying to interpret it and just
     * return {@link #UNKNOWN_BLOB_LABEL}
     */
    private static final int MAX_BLOB_LENGTH = 512;

    /**
     * Label to use when a BLOB column cannot be converted to a string.
     */
    private static final String UNKNOWN_BLOB_LABEL = "{blob}";

    private DatabaseDriver mDatabaseDriver;

    private static final String QUERY_COUNT_TABLE_SQL = "SELECT count(*) FROM %s";
    private static final String PAGE_TABLE_SQL = "SELECT * FROM %s %s LIMIT '%s' OFFSET '%s'";

    public DatabaseProvider(Context context) {
        mDatabaseDriver = (new SqliteDatabaseDriver(context, new DefaultDatabaseFilesProvider(context), new DefaultDatabaseConnectionProvider()));
    }

    public JSONArray getDatabaseNamesJson() throws JSONException {
        JSONArray jsonArray = new JSONArray();
        List<DatabaseDescriptor> databaseNames = getDatabaseNames();
        for (int i = 0; i < databaseNames.size(); i++) {
            jsonArray.put(databaseNames.get(i).name());
        }
        return jsonArray;
    }

    public String getDatabaseNamesTableText() {
        ConsoleTable consoleTable = new ConsoleTable(1, true);
        consoleTable.appendRow();
        consoleTable.appendColumn("database_name");
        List<DatabaseDescriptor> databaseNames = getDatabaseNames();
        for (int i = 0; i < databaseNames.size(); i++) {
            consoleTable.appendRow();
            consoleTable.appendColumn(databaseNames.get(i).name());
        }
        return consoleTable.toString();
    }

    public JSONArray getTableNamesJson(@NonNull final String name) throws JSONException {
        DatabaseDescriptor descriptor = getDatabaseDescriptor(name);
        JSONArray jsonArray = new JSONArray();
        if (descriptor == null) {
            return jsonArray;
        }

        List<String> tableNames = getTableNames(descriptor);
        for (int i = 0; i < tableNames.size(); i++) {
            jsonArray.put(tableNames.get(i));
        }
        return jsonArray;
    }

    public String getTableNamesTableText(@NonNull final String name) {
        DatabaseDescriptor descriptor = getDatabaseDescriptor(name);
        ConsoleTable consoleTable = new ConsoleTable(1, true);
        consoleTable.appendRow();
        consoleTable.appendColumn("table_name");
        if (descriptor == null) {
            return consoleTable.toString();
        }

        List<String> tableNames = getTableNames(descriptor);
        for (int i = 0; i < tableNames.size(); i++) {
            consoleTable.appendRow();
            consoleTable.appendColumn(tableNames.get(i));
        }
        return consoleTable.toString();
    }

    public JSONObject getExecuteSQLResponseJson(@NonNull final String name, @NonNull final String query) throws JSONException {
        ExecuteSQLResponse executeSQLResponse = getExecuteSQLResponse(name, query);

        JSONObject jsonObject = new JSONObject();

        if (executeSQLResponse == null) {
            return jsonObject;
        }

        if (executeSQLResponse.values != null && executeSQLResponse.columnNames != null) {
            JSONArray columnNamesJsonArray = new JSONArray();
            for (String columnName : executeSQLResponse.columnNames) {
                columnNamesJsonArray.put(columnName);
            }
            jsonObject.put("columnNames", columnNamesJsonArray);

            JSONArray valuesJsonArray = new JSONArray();
            JSONArray array = null;
            int size = executeSQLResponse.columnNames.size();
            for (int i = 0; i < executeSQLResponse.values.size(); i++) {
                String s = executeSQLResponse.values.get(i);
                if (i % size == 0) {
                    array = new JSONArray();
                    valuesJsonArray.put(array);
                }
                if (array != null) {
                    array.put(s);
                }
            }
            jsonObject.put("values", valuesJsonArray);
        }
        JSONObject executeSQLResponseJSONObject = new JSONObject();

        if (executeSQLResponse.sqlError != null) {
            executeSQLResponseJSONObject.put("result", executeSQLResponse.sqlError.message);
        } else {
            executeSQLResponseJSONObject.put("result", executeSQLResponse.result == null ? jsonObject : executeSQLResponse.result);
        }

        return executeSQLResponseJSONObject;
    }

    public String getExecuteSQLResponseTableText(String name, String query) {
        ExecuteSQLResponse executeSQLResponse = getExecuteSQLResponse(name, query);

        if (executeSQLResponse == null) {
            return null;
        }

        String result = null;
        if (executeSQLResponse.values != null && executeSQLResponse.columnNames != null) {
            int size = executeSQLResponse.columnNames.size();
            ConsoleTable consoleTable = new ConsoleTable(size, true);
            consoleTable.appendRow();
            for (String columnName : executeSQLResponse.columnNames) {
                consoleTable.appendColumn(columnName);
            }

            for (int i = 0; i < executeSQLResponse.values.size(); i++) {
                String s = executeSQLResponse.values.get(i);
                if (i % size == 0) {
                    consoleTable.appendRow();
                }
                consoleTable.appendColumn(s);
            }
            result = consoleTable.toString();
        }

        if (executeSQLResponse.sqlError != null) {
            result = executeSQLResponse.sqlError.toString();
        }

        return result;
    }

    public String getExecuteSQLResponsePageInfo(String name, String table, String query, int selectLimitSize, int page) {
        ExecuteSQLResponse executeSQLResponse = getExecuteSQLResponse(name, String.format(QUERY_COUNT_TABLE_SQL, table));
        String tableName = "table_name: " + table;
        StringBuilder stringBuilder = new StringBuilder(tableName);
        if (executeSQLResponse == null) {
            stringBuilder.append(LogcatProvider.LINE_BREAK);
            return stringBuilder.toString();
        }

        if (executeSQLResponse.values != null && executeSQLResponse.columnNames != null) {
            int count = Integer.valueOf(executeSQLResponse.values.get(0));
            stringBuilder.append("   第").append(page).append("/").append((count / selectLimitSize) + 1).append("页");
            stringBuilder.append("   （共").append(count).append("条）");
            stringBuilder.append(LogcatProvider.LINE_BREAK);
            stringBuilder.append(getExecuteSQLResponseTableText(name, String.format(PAGE_TABLE_SQL, table, query, selectLimitSize,
                    page > 1 ? (page - 1) * selectLimitSize : 0)));
        }

        return stringBuilder.append(LogcatProvider.LINE_BREAK).toString();
    }

    private DatabaseDescriptor getDatabaseDescriptor(String name) {
        DatabaseDescriptor descriptor;
        List<DatabaseDescriptor> databaseNames = getDatabaseNames();
        for (int i = 0; i < databaseNames.size(); i++) {
            descriptor = databaseNames.get(i);
            if (descriptor.name().equals(name)) {
                return descriptor;
            }
        }
        return null;
    }

    private List<DatabaseDescriptor> getDatabaseNames() {
        return mDatabaseDriver.getDatabaseNames();
    }

    private List<String> getTableNames(DatabaseDescriptor desc) {
        return mDatabaseDriver.getTableNames(desc);
    }

    private ExecuteSQLResponse getExecuteSQLResponse(String name, String query) {
        DatabaseDescriptor databaseDescriptor = getDatabaseDescriptor(name);
        if (databaseDescriptor == null) {
            return null;
        }
        try {
            return mDatabaseDriver.executeSQL(databaseDescriptor, query, new BaseDatabaseDriver.ExecuteResultHandler<ExecuteSQLResponse>() {
                @Override
                public ExecuteSQLResponse handleRawQuery() throws SQLiteException {
                    ExecuteSQLResponse response = new ExecuteSQLResponse();
                    // This is done because the inspector UI likes to delete rows if you give them no
                    // name/value list
                    response.columnNames = Collections.singletonList("success");
                    response.values = Collections.singletonList("true");
                    return response;
                }

                @Override
                public ExecuteSQLResponse handleSelect(Cursor result) throws SQLiteException {
                    ExecuteSQLResponse response = new ExecuteSQLResponse();
                    response.columnNames = Arrays.asList(result.getColumnNames());
                    response.values = flattenRows(result, MAX_EXECUTE_RESULTS);
                    return response;
                }

                @Override
                public ExecuteSQLResponse handleInsert(long insertedId) throws SQLiteException {
                    ExecuteSQLResponse response = new ExecuteSQLResponse();
                    response.result = String.format("ID %s of last inserted row", insertedId);
                    return response;
                }

                @Override
                public ExecuteSQLResponse handleUpdateDelete(int count) throws SQLiteException {
                    ExecuteSQLResponse response = new ExecuteSQLResponse();
                    response.result = String.format("Modified %s rows", count);
                    return response;
                }
            });
        } catch (Throwable e) {
            Logcat.e().tag("").msg(e.getCause().getMessage()).format("Exception executing: %s", query).out();

            Error error = new Error();
            error.code = 0;
            error.message = e.getMessage();
            ExecuteSQLResponse response = new ExecuteSQLResponse();
            response.sqlError = error;
            return response;
        }
    }

    public static class Error {
        public String message;

        public int code;

        @Override
        public String toString() {
            return "Error{" +
                    "message='" + message + '\'' +
                    ", code=" + code +
                    '}';
        }
    }


    private static ArrayList<String> flattenRows(Cursor cursor, @IntRange(from = 0) int limit) {
        ArrayList<String> flatList = new ArrayList<>();
        final int numColumns = cursor.getColumnCount();
        for (int row = 0; row < limit && cursor.moveToNext(); row++) {
            for (int column = 0; column < numColumns; column++) {
                switch (cursor.getType(column)) {
                    case Cursor.FIELD_TYPE_NULL:
                        flatList.add(null);
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        flatList.add(String.valueOf(cursor.getLong(column)));
                        break;
                    case Cursor.FIELD_TYPE_FLOAT:
                        flatList.add(String.valueOf(cursor.getDouble(column)));
                        break;
                    case Cursor.FIELD_TYPE_BLOB:
                        flatList.add(blobToString(cursor.getBlob(column)));
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                    default:
                        flatList.add(cursor.getString(column));
                        break;
                }
            }
        }
        if (!cursor.isAfterLast()) {
            for (int column = 0; column < numColumns; column++) {
                flatList.add("{truncated}");
            }
        }
        return flatList;
    }

    private static String blobToString(byte[] blob) {
        if (blob.length <= MAX_BLOB_LENGTH) {
            if (fastIsAscii(blob)) {
                try {
                    return new String(blob, "US-ASCII");
                } catch (UnsupportedEncodingException e) {
                    // Fall through...
                }
            }
        }
        return UNKNOWN_BLOB_LABEL;
    }

    private static boolean fastIsAscii(byte[] blob) {
        for (byte b : blob) {
            if ((b & ~0x7f) != 0) {
                return false;
            }
        }
        return true;
    }

    public static class ExecuteSQLResponse {
        public List<String> columnNames;

        public List<String> values;

        public String result;

        public Error sqlError;
    }


}
