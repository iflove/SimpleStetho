package com.roogle.simple.stetho.common;

import java.util.ArrayList;
import java.util.List;

public class ConsoleTable {

    public static final String APART_LINE_STR = String.valueOf("|\n");
    public static final String APART_STR = String.valueOf("|");
    public static final char C = '=';
    public static final char C1 = '-';
    private List<List> rows = new ArrayList<>();

    private int column;

    private int[] columnLen;

    private static int margin = 2;

    private boolean printHeader = false;

    public ConsoleTable(int column, boolean printHeader) {
        this.printHeader = printHeader;
        this.column = column;
        this.columnLen = new int[column];
    }

    public void appendRow() {
        List row = new ArrayList(column);
        rows.add(row);
    }

    public ConsoleTable appendColumn(Object value) {
        if (value == null) {
            value = "NULL";
        }
        List row = rows.get(rows.size() - 1);
        row.add(value);
        int len = value.toString().getBytes().length;
        if (columnLen[row.size() - 1] < len) {
            columnLen[row.size() - 1] = len;
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        int sumLen = 0;
        for (int len : columnLen) {
            sumLen += len;
        }
        if (printHeader) {
            buf.append(APART_STR).append(printChar(C, sumLen + margin * 2 * column + (column - 1))).append(APART_LINE_STR);
        } else {
            buf.append(APART_STR).append(printChar(C1, sumLen + margin * 2 * column + (column - 1))).append(APART_LINE_STR);
        }
        for (int ii = 0; ii < rows.size(); ii++) {
            List row = rows.get(ii);
            for (int i = 0; i < column; i++) {
                String o = "";
                if (i < row.size()) {
                    o = row.get(i).toString();
                }
                buf.append('|').append(printChar(' ', margin)).append(o);
                buf.append(printChar(' ', columnLen[i] - o.getBytes().length + margin));
            }
            buf.append(APART_LINE_STR);
            if (printHeader && ii == 0) {
                buf.append(APART_STR).append(printChar(C, sumLen + margin * 2 * column + (column - 1))).append(APART_LINE_STR);
            } else {
                buf.append(APART_STR).append(printChar(C1, sumLen + margin * 2 * column + (column - 1))).append(APART_LINE_STR);
            }
        }
        return buf.toString();
    }

    private String printChar(char c, int len) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < len; i++) {
            buf.append(c);
        }
        return buf.toString();
    }

}