package com.roogle.simple.stetho;

import android.content.Context;
import android.os.Environment;

import com.roogle.simple.stetho.common.DateUtil;
import com.roogle.simple.stetho.common.Consumer;
import com.roogle.simple.stetho.common.StorageUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class LogcatProvider {

    public static String LOGS_FOLDER = "logs";
    public static int MAX_SEND_LENGTH = 1024 * 1024;
    public static long FILE_SAVE_TIME = TimeUnit.DAYS.toMillis(30);
    public static long ALLOWED_QUERY_TIME = TimeUnit.DAYS.toMillis(1);

    static final String LINE_BREAK = "\n";
    private final Context context;
    private static final String FILE_EXTENSION = ".txt";
    private ExecutorService gatherLogExecutorService = Executors.newSingleThreadExecutor();
    private ExecutorService readLogExecutorService = Executors.newSingleThreadExecutor();
    private Process process = null;
    private File logsFile;
    private Future<?> gatherLogExecutorServiceFuture;

    public LogcatProvider(Context context) {
        this.context = context;
    }

    private synchronized File getLogFile() {
        Date date = new Date();
        String formatToday = DateUtil.format(date, DateUtil.SIMPLE_DATE_FORMAT_PATTERN);
        if (logsFile != null) {
            boolean contains = formatToday.contains(logsFile.getName());
            if (contains) {
                //reusing more efficiency
                return logsFile;
            }
        }
        if (context == null) {
            return null;
        }

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File logFilesExternalCacheDir = StorageUtils.getExternalStorageAppFilesFolder(this.context, LOGS_FOLDER);

            File[] listFiles = logFilesExternalCacheDir.listFiles();

            for (int i = 0; i < listFiles.length; i++) {
                File file = listFiles[i];
                String name = file.getName();
                String fileDateStr = name.substring(0, name.length() - FILE_EXTENSION.length());
                Date fileDate = null;
                try {
                    fileDate = DateUtil.parse(fileDateStr, DateUtil.SIMPLE_DATE_FORMAT_PATTERN);
                    if (date.getTime() - fileDate.getTime() >= FILE_SAVE_TIME) {
                        file.delete();
                    }
                } catch (ParseException e) {
                    // DO SOMETHING
                }
            }
            return new File(logFilesExternalCacheDir, formatToday + FILE_EXTENSION);
        }
        return null;
    }

    public String getUtcTimeString() {
        return DateUtil.format(new Date(), DateUtil.UTC_DATE_FORMAT_PATTERN);
    }

    public void startGatherLogcatInfo() {
        gatherLogExecutorServiceFuture = gatherLogExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                InputStream inputStream = null;
                try {
                    destroyProcess();
                    Runtime.getRuntime().exec("logcat -c");
                    //-v time 显示log 时间 GMT
                    process = Runtime.getRuntime().exec("logcat -v time | grep " + android.os.Process.myPid());
                    inputStream = process.getInputStream();
                    InputStreamReader reader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(reader);

                    FileWriter out = null;
                    BufferedWriter bufferedWriter = null;

                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        File dateFile = getLogFile();
                        if (logsFile == null) {
                            logsFile = dateFile;
                            out = new FileWriter(logsFile, logsFile.exists());
                            bufferedWriter = new BufferedWriter(out);
                        }

                        if (logsFile != null && dateFile != null) {
                            boolean isSameFile = logsFile.getName().equals(dateFile.getName());
                            logsFile = dateFile;
                            if (!isSameFile || !logsFile.exists()) {
                                out = new FileWriter(logsFile, logsFile.exists());
                                bufferedWriter = new BufferedWriter(out);
                            }
                        }

                        if (bufferedWriter != null) {
                            bufferedWriter.write(getUtcTimeString());
                            bufferedWriter.write(": ");
                            bufferedWriter.write(line);
                            bufferedWriter.write(LINE_BREAK);
                            bufferedWriter.flush();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    destroyProcess();
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            // DO SOMETHING
                        }
                    }
                }
            }
        });
    }

    public void destroyProcess() {
        if (process != null) {
            process.destroy();
        }
    }

    private void readLogcatLogFiles(final Long startTime, final Long endTime, final List<File> fileList, final Consumer<String> callBack) {
        if (fileList.isEmpty()) {
            callBack.apply("Error: Can't find logs");
            return;
        }
        readLogExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                try {
                    for (File file : fileList) {
                        FileInputStream fileInputStream = new FileInputStream(file);
                        InputStreamReader reader = new InputStreamReader(fileInputStream);
                        BufferedReader bufferedReader = new BufferedReader(reader);
                        String line;
                        StringBuilder stringBuffer = new StringBuilder(file.getName());
                        stringBuffer.append(LINE_BREAK);
                        while ((line = bufferedReader.readLine()) != null) {
                            try {
                                String date = line.substring(0, 25);
                                Date parseDate = DateUtil.parse(date, DateUtil.UTC_DATE_FORMAT_PATTERN);
                                long parseDateTime = parseDate.getTime();
                                if (parseDateTime >= startTime && parseDateTime <= endTime) {
                                    stringBuffer.append(line);
                                    stringBuffer.append(LINE_BREAK);
                                }
                                if (parseDateTime > endTime) {
                                    break;
                                }
                                if (stringBuffer.length() >= MAX_SEND_LENGTH) {
                                    stringBuffer.append("{log truncated}");
                                    stringBuffer.append(LINE_BREAK);
                                    stringBuffer.append("Please pass through (begin end) shorten the query time range ");
                                    break;
                                }
                            } catch (ParseException e) {
                                // DO SOMETHING
                            }
                        }
                        String logContent = stringBuffer.toString();
                        if (!file.exists()) {
                            stringBuffer.append("Error: Can't find logs");
                            stringBuffer.append(LINE_BREAK);
                            logContent = stringBuffer.toString();
                        }
                        if (callBack != null) {
                            callBack.apply(logContent);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (callBack != null) {
                        callBack.apply("Error: occur IOException");
                    }
                }
            }
        });
    }

    public void readLogcatLogFiles(final String start, final String end, final Consumer<String> callBack) {
        try {
            long startDateTime = 0;
            long endDateTime = 0;
            Date startDate = DateUtil.parse(start, DateUtil.UTC_DATE_FORMAT_PATTERN);
            Date endDate = DateUtil.parse(end, DateUtil.UTC_DATE_FORMAT_PATTERN);
            startDateTime = startDate.getTime();
            endDateTime = endDate.getTime();

            if (endDateTime <= startDateTime) {
                callBack.apply("Error: begin <= end ");
                return;
            }

            if (endDateTime - startDateTime > ALLOWED_QUERY_TIME) {
                callBack.apply("Error: More than 1 days is not allowed");
                return;
            }

            long startDateFileTime = DateUtil.parse(start, DateUtil.SIMPLE_DATE_FORMAT_PATTERN).getTime();
            long endDateFileTime = DateUtil.parse(end, DateUtil.SIMPLE_DATE_FORMAT_PATTERN).getTime();

            readLogcatLogFiles(startDateTime, endDateTime, listLogFile(startDateFileTime, endDateFileTime), callBack);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private synchronized List<File> listLogFile(final Long startTime, final Long endTime) {
        ArrayList<File> fileArrayList = new ArrayList<>();
        List<File> files = Collections.unmodifiableList(fileArrayList);
        if (context == null) {
            return null;
        }

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

            File logFilesExternalCacheDir = StorageUtils.getExternalStorageAppFilesFolder(this.context, LOGS_FOLDER);
            if (logFilesExternalCacheDir == null) {
                return null;
            }
            File[] listFiles = logFilesExternalCacheDir.listFiles();

            for (int index = listFiles.length - 1; index >= 0; index--) {
                File file = listFiles[index];
                String name = file.getName();
                String fileDateStr = name.substring(0, name.length() - FILE_EXTENSION.length());
                Date fileDate = null;
                try {
                    fileDate = DateUtil.parse(fileDateStr, DateUtil.SIMPLE_DATE_FORMAT_PATTERN);
                    long fileDateTime = fileDate.getTime();
                    if (startTime <= fileDateTime && endTime >= fileDateTime) {
                        fileArrayList.add(file);
                    }
                } catch (ParseException e) {
                    // DO SOMETHING
                }
            }
        }
        return files;
    }

    void readTracesLogFile(final Consumer<String> callBack) {
        final File file = new File("/data/anr/traces.txt");
        if (!file.exists() || !file.canRead()) {
            callBack.apply("Error: Can't find traces.txt");
            return;
        }
        readLogExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                try {
                    FileInputStream fileInputStream = new FileInputStream(file);
                    InputStreamReader reader = new InputStreamReader(fileInputStream);
                    BufferedReader bufferedReader = new BufferedReader(reader);
                    String line;
                    StringBuilder stringBuffer = new StringBuilder(file.getName());
                    stringBuffer.append(LINE_BREAK);
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuffer.append(line);
                        stringBuffer.append(LINE_BREAK);
                    }
                    if (callBack != null) {
                        callBack.apply(stringBuffer.toString());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (callBack != null) {
                        callBack.apply("Error: occur IOException");
                    }
                }
            }
        });

    }

    public void stopGatherLogcatInfo() {
        gatherLogExecutorServiceFuture.cancel(true);
    }

}
