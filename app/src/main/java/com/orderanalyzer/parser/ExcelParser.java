package com.orderanalyzer.parser;

import com.orderanalyzer.model.OrderRow;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel (.xlsx) parser that uses fixed column indexes,
 * safe numeric parsing, and skips empty/invalid rows.
 */
public class ExcelParser {

    /**
     * Parses the Excel file using fixed indexes:
     * - Retailer Name: index 4 (Column E)
     * - Vehicle: index 5 (Column F)
     * - Products: index 7 to end (Column H onwards)
     */
    public static List<OrderRow> parse(File file) throws Exception {
        List<OrderRow> rows = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            if (sheet == null || sheet.getLastRowNum() < 0) {
                throw new Exception("The Excel file is empty or has no sheets.");
            }

            // Start from row 1 to skip the header row (row 0)
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                
                // Skip if entire row object is null or physically blank
                if (row == null || isBlankRow(row, formatter, evaluator)) {
                    continue;
                }

                // Fixed Indexes
                String retailer = safeCellText(row.getCell(4), formatter, evaluator).trim();
                String vehicle  = safeCellText(row.getCell(5), formatter, evaluator).trim();

                // Skip row if Retailer name is empty
                if (retailer.isEmpty()) {
                    continue;
                }

                if (vehicle.isEmpty()) {
                    vehicle = "Unknown Vehicle";
                }

                OrderRow orderRow = new OrderRow();
                orderRow.setRetailerName(retailer);
                orderRow.setVehicleName(vehicle);

                // Row total calculation: SUM of columns from index 7 to last column
                int lastCol = row.getLastCellNum();
                for (int i = 7; i < lastCol; i++) {
                    String cellValue = safeCellText(row.getCell(i), formatter, evaluator).trim();
                    if (!cellValue.isEmpty()) {
                        try {
                            double qty = Double.parseDouble(cellValue);
                            orderRow.addProduct("Product " + i, qty);
                        } catch (Exception e) {
                            // ignore non-numeric values
                        }
                    }
                }

                rows.add(orderRow);
            }
        }

        if (rows.isEmpty()) {
            throw new Exception("No valid data rows found.");
        }
        
        return rows;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the display text of a cell regardless of its type.
     */
    private static String safeCellText(Cell cell, DataFormatter fmt, FormulaEvaluator ev) {
        if (cell == null) return "";
        try {
            if (cell.getCellType() == CellType.FORMULA) {
                try {
                    return fmt.formatCellValue(cell, ev).trim();
                } catch (Exception e) {
                    try   { return String.valueOf(cell.getNumericCellValue()); }
                    catch (Exception e2) { return cell.getStringCellValue().trim(); }
                }
            }
            return fmt.formatCellValue(cell).trim();
        } catch (Exception e) {
            return "";
        }
    }

    /** Returns true if every cell in the row is null or whitespace-only. */
    private static boolean isBlankRow(Row row, DataFormatter fmt, FormulaEvaluator ev) {
        int lastCol = row.getLastCellNum();
        for (int c = 0; c < lastCol; c++) {
            if (!safeCellText(row.getCell(c), fmt, ev).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
