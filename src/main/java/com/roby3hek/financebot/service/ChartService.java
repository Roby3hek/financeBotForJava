package com.roby3hek.financebot.service;

import com.roby3hek.financebot.dto.StatsDto;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Map;

@Service
public class ChartService {
    
    public byte[] createExpensePieChart(StatsDto stats) throws Exception {
        PieChart chart = new PieChartBuilder()
            .width(800)
            .height(600)
            .title("Расходы по категориям")
            .build();
        
        for (Map.Entry<String, BigDecimal> entry : stats.getExpensesByCategory().entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                chart.addSeries(entry.getKey(), entry.getValue().doubleValue());
            }
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitmapEncoder.saveBitmap(chart, baos, BitmapEncoder.BitmapFormat.PNG);
        return baos.toByteArray();
    }
    
    public byte[] createIncomePieChart(StatsDto stats) throws Exception {
        PieChart chart = new PieChartBuilder()
            .width(800)
            .height(600)
            .title("Доходы по категориям")
            .build();
        
        for (Map.Entry<String, BigDecimal> entry : stats.getIncomeByCategory().entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                chart.addSeries(entry.getKey(), entry.getValue().doubleValue());
            }
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitmapEncoder.saveBitmap(chart, baos, BitmapEncoder.BitmapFormat.PNG);
        return baos.toByteArray();
    }
}