package com.roogle.simple.stetho.common;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;

import java.io.File;

public final class StorageUtils {

    private static final String DIR_ANDROID = "Android";
    private static final String DIR_DATA = "data";
    private static final String DIR_FILES = "files";
    private static final String DIR_CACHE = "cache";

    @Nullable
    public static synchronized File getExternalStorageAppFilesFolder(Context context, String folderName) {
        if (context == null) {
            return null;
        }
        if (folderName == null) {
            return null;
        }
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File dirs = buildExternalStorageAppFilesDirs(Environment.getExternalStorageDirectory().getAbsolutePath(), context.getPackageName());
            File file = new File(dirs, folderName);
            if (!file.exists()) {
                file.mkdir();
            }
            return file;
        }
        return null;
    }

    public synchronized static File buildExternalStorageAppFilesDirs(String externalStoragePath, String packageName) {
        return buildPath(externalStoragePath, DIR_ANDROID, DIR_DATA, packageName, DIR_FILES);
    }

    public synchronized static File buildPath(String base, String... segments) {
        File cur = new File(base);
        for (String segment : segments) {
            cur = new File(cur, segment);
            if (!cur.exists()) {
                cur.mkdir();
            }
        }
        return cur;
    }
}
