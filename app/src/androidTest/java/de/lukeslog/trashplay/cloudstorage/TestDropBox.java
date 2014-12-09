package de.lukeslog.trashplay.cloudstorage;

import android.test.InstrumentationTestCase;

import org.joda.time.DateTime;

public class TestDropBox extends InstrumentationTestCase{

    public void testDateConversion() {
        String modifiedDate = "Tue, 01 Dec 2014 18:51:26 +0000";
        DropBox d = new DropBox();
        DateTime x = d.getDateTimeFromDropBoxModificationTimeString(modifiedDate);
        assertEquals(1, x.getDayOfWeek());
        assertEquals(2014, x.getYear());
    }


}
