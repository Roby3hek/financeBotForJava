package com.roby3hek.financebot.controller;

import com.roby3hek.financebot.dto.StatsDto;
import com.roby3hek.financebot.dto.TransactionDto;
import com.roby3hek.financebot.entity.Transaction;
import com.roby3hek.financebot.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<String> create(@RequestBody TransactionDto dto, Authentication auth) {
        Long telegramId = (Long) auth.getPrincipal();
        transactionService.addTransaction(telegramId, dto.getAmount(), dto.getType().name(), dto.getCategory(), dto.getDescription());
        return ResponseEntity.ok("Операция добавлена");
    }

    @GetMapping
    public List<Transaction> getAll(Authentication auth) {
        Long telegramId = (Long) auth.getPrincipal();
        return transactionService.getUserTransactions(telegramId);
    }

    @GetMapping("/stats")
    public StatsDto getStats(Authentication auth) {
        Long telegramId = (Long) auth.getPrincipal();
        com.roby3hek.financebot.dto.StatsDto stats = transactionService.getStats(telegramId);
        
        // Конвертируем BigDecimal в double для ответа
        return StatsDto.builder()
            .totalIncome(stats.getTotalIncome())
            .totalExpense(stats.getTotalExpense())
            .balance(stats.getBalance())
            .expensesByCategory(stats.getExpensesByCategory())
            .incomeByCategory(stats.getIncomeByCategory())
            .transactionCount(stats.getTransactionCount())
            .averageTransaction(stats.getAverageTransaction())
            .largestIncome(stats.getLargestIncome())
            .largestExpense(stats.getLargestExpense())
            .build();
    }
}