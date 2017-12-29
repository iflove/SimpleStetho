package com.roogle.simple.stetho.common;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class ConsoleTable {

    private static final String APART_STR = String.valueOf("|");
    private static final String APART_STR_LINE = String.valueOf("|\n");
    private static final String HEADER_LINE_SMALL = "=";
    private static final String BORDER_LINE_SMALL = "-";
    private List<List<String>> rows = new ArrayList<>();

    private int column;

    private int[] columnLen;

    private int leftRightMargin = 2;

    private boolean printHeader = false;

    public ConsoleTable(int column, boolean printHeader) {
        this.printHeader = printHeader;
        this.column = column;
        this.columnLen = new int[column];
    }

    public void appendRow() {
        rows.add(new ArrayList<String>(column));
    }

    public ConsoleTable appendColumn(Object value) {
        if (value == null) {
            value = "NULL";
        }
        List<String> lastRow = rows.get(rows.size() - 1);
        lastRow.add(value.toString());
        int len = 0;
        try {
            len = value.toString().getBytes("gbk").length;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (columnLen[lastRow.size() - 1] < len) {
            columnLen[lastRow.size() - 1] = len;
        }
        return this;
    }

    public void setLeftRightMargin(int leftRightMargin) {
        this.leftRightMargin = leftRightMargin;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        int totalLen = 0;
        for (int len : columnLen) {
            totalLen += len;
        }
        int len = totalLen + leftRightMargin * 2 * column + (column - 1);
        if (printHeader) {
            buf.append(APART_STR).append(printChar(HEADER_LINE_SMALL, len)).append(APART_STR_LINE);
        } else {
            buf.append(APART_STR).append(printChar(BORDER_LINE_SMALL, len)).append(APART_STR_LINE);
        }
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            for (int i = 0; i < column; i++) {
                String s = "";
                if (i < row.size()) {
                    s = row.get(i);
                }
                buf.append('|').append(printChar(" ", leftRightMargin)).append(s);
                try {
                    buf.append(printChar(" ", columnLen[i] - s.getBytes("gbk").length + leftRightMargin));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            buf.append(APART_STR_LINE);
            if (printHeader && rowIndex == 0) {
                buf.append(APART_STR).append(printChar(HEADER_LINE_SMALL, len)).append(APART_STR_LINE);
            } else {
                buf.append(APART_STR).append(printChar(BORDER_LINE_SMALL, len)).append(APART_STR_LINE);
            }
        }
        return buf.toString();
    }

    private String printChar(String c, int len) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < len; i++) {
            buf.append(c);
        }
        return buf.toString();
    }

}