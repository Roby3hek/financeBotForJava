package com.roby3hek.financebot.service;

import com.opencsv.CSVWriter;
import com.roby3hek.financebot.entity.Transaction;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    
    public byte[] exportToExcel(List<Transaction> transactions) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Транзакции");
        
        // Создаем стиль для заголовков
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // Заголовки
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Дата", "Тип", "Категория", "Сумма", "Описание"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.autoSizeColumn(i);
        }
        
        // Данные
        int rowNum = 1;
        for (Transaction t : transactions) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(t.getCreatedAt().format(DATE_FORMATTER));
            row.createCell(1).setCellValue(t.getType().toString());
            row.createCell(2).setCellValue(t.getCategory());
            row.createCell(3).setCellValue(t.getAmount());
            row.createCell(4).setCellValue(t.getDescription() != null ? t.getDescription() : "");
        }
        
        // Итоговая строка
        Row summaryRow = sheet.createRow(rowNum + 1);
        summaryRow.createCell(2).setCellValue("ИТОГО:");
        double total = transactions.stream()
            .mapToDouble(t -> t.getType().toString().equals("INCOME") ? t.getAmount() : -t.getAmount())
            .sum();
        summaryRow.createCell(3).setCellValue(total);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return baos.toByteArray();
    }
    
    public byte[] exportToCsv(List<Transaction> transactions) throws Exception {
        StringWriter sw = new StringWriter();
        CSVWriter writer = new CSVWriter(sw);
        
        // Заголовки
        writer.writeNext(new String[]{"Дата", "Тип", "Категория", "Сумма", "Описание"});
        
        // Данные
        for (Transaction t : transactions) {
            writer.writeNext(new String[]{
                t.getCreatedAt().format(DATE_FORMATTER),
                t.getType().toString(),
                t.getCategory(),
                t.getAmount().toString(),
                t.getDescription() != null ? t.getDescription() : ""
            });
        }
        
        writer.close();
        return sw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}