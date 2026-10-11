package com.orderanalyzer.parser;

import com.orderanalyzer.model.OrderRow;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParserTest {

    @Test
    public void parseCsv_aggregatesProductColumnsAndHandlesMissingVehicle() throws Exception {
        String csv = "A,B,C,D,Retailer,Vehicle,G,Product 1,Product 2\n"
                + "1,2,3,4,Alpha Store,Vehicle A,,2,3.5\n"
                + "1,2,3,4,Beta Store,,,4,invalid\n"
                + "1,2,3,4,,Vehicle C,,9,9\n";

        List<OrderRow> rows = CsvParser.parse(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, rows.size());
        assertEquals("Alpha Store", rows.get(0).getRetailerName());
        assertEquals("Vehicle A", rows.get(0).getVehicleName());
        assertEquals(5.5, rows.get(0).getRowTotal(), 0.0);
        assertEquals(2, rows.get(0).getProductQuantities().size());
        assertEquals("Unknown Vehicle", rows.get(1).getVehicleName());
        assertEquals(4.0, rows.get(1).getRowTotal(), 0.0);
        assertTrue(rows.get(1).getProductQuantities().containsKey("Product 7"));
    }

    @Test
    public void parseExcel_supportsLegacyXlsFiles() throws Exception {
        File file = File.createTempFile("order-analyzer-", ".xls");
        try (Workbook workbook = new HSSFWorkbook();
             FileOutputStream output = new FileOutputStream(file)) {
            Sheet sheet = workbook.createSheet("Orders");
            Row header = sheet.createRow(0);
            header.createCell(4).setCellValue("Retailer");
            header.createCell(5).setCellValue("Vehicle");
            header.createCell(7).setCellValue("Product");

            Row order = sheet.createRow(1);
            order.createCell(4).setCellValue("Alpha Store");
            order.createCell(5).setCellValue("Vehicle A");
            order.createCell(7).setCellValue(12.0);
            workbook.write(output);
        }

        try {
            List<OrderRow> rows = ExcelParser.parse(file);
            assertEquals(1, rows.size());
            assertEquals("Alpha Store", rows.get(0).getRetailerName());
            assertEquals(12.0, rows.get(0).getRowTotal(), 0.0);
        } finally {
            assertTrue(file.delete());
        }
    }
}
