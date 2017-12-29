package com.facebook.stetho;

import com.roogle.simple.stetho.common.ConsoleTable;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
        ConsoleTable consoleTable = new ConsoleTable(2, true);
        consoleTable.appendRow();
        consoleTable.appendColumn("_id");
        consoleTable.appendColumn("url");
        consoleTable.appendRow();
        consoleTable.appendColumn("晨星猫山一号");
        consoleTable.appendColumn("http://10053145.file.");
        System.out.println(consoleTable.toString());
    }
}