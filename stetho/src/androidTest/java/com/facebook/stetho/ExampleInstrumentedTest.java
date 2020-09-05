package com.facebook.stetho;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.lazy.library.logging.Logcat;
import com.roogle.simple.stetho.SimpleStetho;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    private static final String TAG = "ExampleInstrumentedTest";
    SimpleStetho simpleStetho;

    @Before
    public void setup() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.facebook.stetho.test", appContext.getPackageName());

        simpleStetho = new SimpleStetho(appContext);
    }

    @Test
    public void testGetDatabaseNames() throws Exception {
        Logcat.i().ln().msg(simpleStetho.getDatabaseProvider().getDatabaseNamesTableText()).out();
        Logcat.i().tag(TAG).fmtJSON(simpleStetho.getDatabaseProvider().getDatabaseNamesJson().toString());
    }

    @Test
    public void testGetTableNames() throws Exception {
        Logcat.i().ln().msg(simpleStetho.getDatabaseProvider().getTableNamesTableText("youzi.db")).out();
//        Logcat.json(TAG, simpleStetho.getDatabaseProvider().getTableNamesJson("zxbox-db").toString());
    }

    @Test
    public void testGetExecuteSQLResponse() throws Exception {
        Logcat.i().ln().msg(simpleStetho.getDatabaseProvider().getExecuteSQLResponseTableText("youzi.db", "select * from product")).out();
//        Logcat.json(TAG, simpleStetho.getDatabaseProvider().getExecuteSQLResponseJson("zxbox-db", "select * from ALBUM").toString());
    }
}
