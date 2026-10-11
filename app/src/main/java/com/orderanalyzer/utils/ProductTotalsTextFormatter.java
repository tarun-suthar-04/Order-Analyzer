package com.orderanalyzer.utils;

import com.orderanalyzer.model.ProductTotalItem;

import java.util.List;

/** Builds the quantity-only text used by the Product Totals copy/share actions. */
public final class ProductTotalsTextFormatter {

    private ProductTotalsTextFormatter() {}

    public static String format(String selectedDate, String todayDate, String yesterdayDate,
                                String displayDate, List<ProductTotalItem> products) {
        String heading;
        if (selectedDate != null && selectedDate.equals(todayDate)) {
            heading = "Today's Product Requirement";
        } else if (selectedDate != null && selectedDate.equals(yesterdayDate)) {
            heading = "Yesterday's Product Requirement";
        } else {
            heading = "Product Requirement";
        }

        StringBuilder text = new StringBuilder(heading)
                .append('\n')
                .append(displayDate == null ? "" : displayDate)
                .append("\n\n");

        int totalQuantity = 0;
        boolean hasProducts = false;
        if (products != null) {
            for (ProductTotalItem product : products) {
                if (product == null || product.getTotalQuantity() <= 0) continue;
                hasProducts = true;
                totalQuantity += product.getTotalQuantity();
                text.append(product.getProductName())
                        .append(" - ")
                        .append(product.getTotalQuantity())
                        .append('\n');
            }
        }

        if (!hasProducts) {
            text.append("No product orders for this date.\n");
        }
        text.append("\nTotal Items - ").append(totalQuantity);
        return text.toString();
    }
}
