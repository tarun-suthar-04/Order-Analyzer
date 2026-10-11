package com.orderanalyzer.utils;

import com.orderanalyzer.model.ProductTotalItem;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProductTotalsTextFormatterTest {

    private static final String TODAY = "2026-10-06";
    private static final String YESTERDAY = "2026-10-05";

    @Test
    public void todayUsesTodayHeadingAndOnlyProductQuantities() {
        String text = ProductTotalsTextFormatter.format(TODAY, TODAY, YESTERDAY, "Tue, 06 Oct 2026",
                Arrays.asList(product(1, "rich tea", 120, 37.0), product(2, "125 gm Milk", 110, 12.0)));

        assertEquals("Today's Product Requirement\nTue, 06 Oct 2026\n\n"
                + "rich tea - 120\n125 gm Milk - 110\n\nTotal Items - 230", text);
        assertContainsNoPriceOrAmount(text);
    }

    @Test
    public void yesterdayUsesYesterdayHeadingAndPreservesProductOrder() {
        String text = ProductTotalsTextFormatter.format(YESTERDAY, TODAY, YESTERDAY, "Mon, 05 Oct 2026",
                Arrays.asList(product(9, "City Special", 35, 18.5), product(4, "Burger Bun", 31, 0)));

        assertTrue(text.startsWith("Yesterday's Product Requirement\nMon, 05 Oct 2026\n\n"));
        assertTrue(text.indexOf("City Special - 35") < text.indexOf("Burger Bun - 31"));
        assertTrue(text.endsWith("Total Items - 66"));
        assertContainsNoPriceOrAmount(text);
    }

    @Test
    public void olderDateUsesGenericHeading() {
        String text = ProductTotalsTextFormatter.format("2026-10-03", TODAY, YESTERDAY, "Sat, 03 Oct 2026",
                Arrays.asList(product(7, "Maruti 500 Gram", 25, 25), product(2, "Gold Coin", 8, 40)));

        assertEquals("Product Requirement\nSat, 03 Oct 2026\n\n"
                + "Maruti 500 Gram - 25\nGold Coin - 8\n\nTotal Items - 33", text);
        assertContainsNoPriceOrAmount(text);
    }

    @Test
    public void emptyDateShowsHelpfulMessageAndZeroTotal() {
        String text = ProductTotalsTextFormatter.format(YESTERDAY, TODAY, YESTERDAY, "Mon, 05 Oct 2026",
                Collections.emptyList());

        assertEquals("Yesterday's Product Requirement\nMon, 05 Oct 2026\n\n"
                + "No product orders for this date.\n\nTotal Items - 0", text);
        assertContainsNoPriceOrAmount(text);
    }

    @Test
    public void zeroQuantityRowsAreOmittedFromCopyOutput() {
        String text = ProductTotalsTextFormatter.format(TODAY, TODAY, YESTERDAY, "Tue, 06 Oct 2026",
                Arrays.asList(product(1, "Ordered", 3, 100), product(2, "Not ordered", 0, 80)));

        assertTrue(text.contains("Ordered - 3"));
        assertFalse(text.contains("Not ordered"));
        assertTrue(text.endsWith("Total Items - 3"));
        assertContainsNoPriceOrAmount(text);
    }

    private ProductTotalItem product(long id, String name, int quantity, double price) {
        return new ProductTotalItem(id, name, quantity, "", "", price);
    }

    private void assertContainsNoPriceOrAmount(String text) {
        assertFalse(text.contains("₹"));
        assertFalse(text.contains("Grand Total Bill"));
        assertFalse(text.contains(" = "));
        assertFalse(text.contains(" × "));
    }
}
