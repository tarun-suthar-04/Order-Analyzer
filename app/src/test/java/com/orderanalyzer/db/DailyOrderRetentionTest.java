package com.orderanalyzer.db;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DailyOrderRetentionTest {

    @Test
    public void retentionConstantIsSevenDays() {
        assertEquals(7, DailyOrderRepository.ORDER_RETENTION_DAYS);
    }

    @Test
    public void getLastNDatesReturnsExactlyRequestedDaysDescending() {
        List<String> dates = DailyOrderRepository.getLastNDates(7);
        assertEquals(7, dates.size());

        // First item must be today
        assertEquals(DailyOrderRepository.getTodayDate(), dates.get(0));

        // Second item must be yesterday
        assertEquals(DailyOrderRepository.getYesterdayDate(), dates.get(1));

        // Dates must be contiguous descending days
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar expectedCal = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            String expected = sdf.format(expectedCal.getTime());
            assertEquals(expected, dates.get(i));
            expectedCal.add(Calendar.DAY_OF_YEAR, -1);
        }
    }

    @Test
    public void retentionCutoffDateIsExactlySixDaysAgo() {
        String cutoff = DailyOrderRepository.getRetentionCutoffDate();
        String expectedSixDaysAgo = DailyOrderRepository.getDateDaysAgo(6);
        assertEquals(expectedSixDaysAgo, cutoff);

        // All 7 dates in the retention window must be >= cutoff
        List<String> retained = DailyOrderRepository.getLastNDates(7);
        for (String d : retained) {
            assertTrue("Date " + d + " should be >= cutoff " + cutoff, d.compareTo(cutoff) >= 0);
        }

        // An order from 7 days ago (or earlier) must be strictly < cutoff
        String sevenDaysAgo = DailyOrderRepository.getDateDaysAgo(7);
        assertTrue("Date " + sevenDaysAgo + " should be < cutoff " + cutoff, sevenDaysAgo.compareTo(cutoff) < 0);

        String eightDaysAgo = DailyOrderRepository.getDateDaysAgo(8);
        assertTrue("Date " + eightDaysAgo + " should be < cutoff " + cutoff, eightDaysAgo.compareTo(cutoff) < 0);
    }

    @Test
    public void relativeLabelIdentifiesTodayYesterdayAndOlderDates() {
        String today = DailyOrderRepository.getTodayDate();
        String yesterday = DailyOrderRepository.getYesterdayDate();
        String twoDaysAgo = DailyOrderRepository.getDateDaysAgo(2);
        String sixDaysAgo = DailyOrderRepository.getDateDaysAgo(6);

        assertEquals("Today", DailyOrderRepository.getRelativeLabel(today));
        assertEquals("Yesterday", DailyOrderRepository.getRelativeLabel(yesterday));

        // Older dates format as "dd MMM"
        SimpleDateFormat sdfParse = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat sdfExpected = new SimpleDateFormat("dd MMM", Locale.US);
        try {
            Date d2 = sdfParse.parse(twoDaysAgo);
            assertEquals(sdfExpected.format(d2), DailyOrderRepository.getRelativeLabel(twoDaysAgo));

            Date d6 = sdfParse.parse(sixDaysAgo);
            assertEquals(sdfExpected.format(d6), DailyOrderRepository.getRelativeLabel(sixDaysAgo));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
