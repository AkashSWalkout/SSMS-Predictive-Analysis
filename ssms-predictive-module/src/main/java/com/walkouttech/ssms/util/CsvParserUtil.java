package com.walkouttech.ssms.util;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.walkouttech.ssms.exception.ApiException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Component
public class CsvParserUtil {

    /**
     * Parses a CSV or Excel file into a list of row maps.
     * Each map is header-column → cell-value.
     */
    public List<Map<String, String>> parse(MultipartFile file) {

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isEmpty()) {
            throw new ApiException("File name is missing", HttpStatus.BAD_REQUEST);
        }

        String lower = originalName.toLowerCase();

        if (lower.endsWith(".csv")) {
            return parseCsv(file);
        } else if (lower.endsWith(".xlsx")) {
            return parseExcel(file);
        } else {
            throw new ApiException(
                    "Unsupported file format. Please upload .csv or .xlsx",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    // ================= CSV =================
    private List<Map<String, String>> parseCsv(MultipartFile file) {
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {

            List<String[]> allRows = reader.readAll();

            if (allRows.isEmpty()) {
                throw new ApiException("CSV file is empty", HttpStatus.BAD_REQUEST);
            }

            String[] headers = allRows.get(0);
            List<Map<String, String>> result = new ArrayList<>();

            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                Map<String, String> rowMap = new LinkedHashMap<>();

                for (int j = 0; j < headers.length; j++) {
                    String value = (j < row.length) ? row[j].trim() : "";
                    rowMap.put(headers[j].trim(), value);
                }

                result.add(rowMap);
            }

            return result;

        } catch (IOException | CsvException e) {
            throw new ApiException("Failed to parse CSV: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ================= EXCEL =================
    private List<Map<String, String>> parseExcel(MultipartFile file) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() == 0) {
                throw new ApiException("Excel file is empty", HttpStatus.BAD_REQUEST);
            }

            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();

            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell).trim());
            }

            List<Map<String, String>> result = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, String> rowMap = new LinkedHashMap<>();

                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    String value = (cell != null) ? getCellValueAsString(cell).trim() : "";
                    rowMap.put(headers.get(j), value);
                }

                result.add(rowMap);
            }

            return result;

        } catch (IOException e) {
            throw new ApiException("Failed to parse Excel: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                // Avoid scientific notation for IDs
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
