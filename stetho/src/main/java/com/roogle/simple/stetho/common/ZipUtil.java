package com.roogle.simple.stetho.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ZipUtil {

    public static String compressString(String str) throws IOException {
        if (null == str || str.length() <= 0) {
            return str;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(64);
        GZIPOutputStream zipOutputStream = new GZIPOutputStream(out);
        zipOutputStream.write(str.getBytes());
        zipOutputStream.close();
        return out.toString("ISO-8859-1");
    }

    public static String unCompressString(String str) throws IOException {
        if (null == str || str.length() <= 0) {
            return str;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(64);
        ByteArrayInputStream in = new ByteArrayInputStream(str.getBytes("ISO-8859-1"));
        GZIPInputStream gzip = new GZIPInputStream(in);
        byte[] buffer = new byte[256];
        int n = 0;
        // 将未压缩数据读入字节数组
        while ((n = gzip.read(buffer)) >= 0) {
            out.write(buffer, 0, n);
        }
        return out.toString("UTF-8");
    }
}