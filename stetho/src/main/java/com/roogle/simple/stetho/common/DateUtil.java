package com.roogle.simple.stetho.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class DateUtil {
    public static final String SIMPLE_DATE_FORMAT_PATTERN = "yyyy-MM-dd";
    public static String UTC_DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private static Map<String, ThreadLocal<SimpleDateFormat>> sdfMap = new HashMap<String, ThreadLocal<SimpleDateFormat>>();
    private static final Object LOCK_OBJ = new Object();

    private static SimpleDateFormat getSdf(final String pattern) {
        ThreadLocal<SimpleDateFormat> tl = sdfMap.get(pattern);

        if (tl == null) {
            synchronized (LOCK_OBJ) {
                tl = sdfMap.get(pattern);
                if (tl == null) {

                    tl = new ThreadLocal<SimpleDateFormat>() {
                        @Override
                        protected SimpleDateFormat initialValue() {
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
                            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            return simpleDateFormat;
                        }
                    };
                    sdfMap.put(pattern, tl);
                }
            }
        }

        return tl.get();
    }

    public static SimpleDateFormat getSimpleDateFormat(String pattern) {
        return getSdf(pattern);
    }

    public static String format(Date date, String pattern) {
        return getSdf(pattern).format(date);
    }

    public static Date parse(String dateStr, String pattern) throws ParseException {
        return getSdf(pattern).parse(dateStr);
    }
}
