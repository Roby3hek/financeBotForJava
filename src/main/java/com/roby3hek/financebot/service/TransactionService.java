package com.roby3hek.financebot.service;

import com.roby3hek.financebot.dto.StatsDto;
import com.roby3hek.financebot.entity.Transaction;
import com.roby3hek.financebot.entity.TransactionType;
import com.roby3hek.financebot.entity.User;
import com.roby3hek.financebot.repository.TransactionRepository;
import com.roby3hek.financebot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final ExportService exportService;
    private final ChartService chartService;

    @Transactional
    public void addTransaction(Long telegramId, Double amount, String typeStr, String category, String description) {
        User user = userService.findByTelegramId(telegramId);
        TransactionType type = TransactionType.valueOf(typeStr.toUpperCase());

        Transaction transaction = Transaction.builder()
                .user(user)
                .amount(amount)
                .type(type)
                .category(category)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
        log.info("Transaction added for user {}: {} {} {} {}", telegramId, type, amount, category, description);
    }

    public Double getBalance(Long telegramId) {
        User user = userService.findByTelegramId(telegramId);
        Double balance = transactionRepository.getBalance(user);
        return balance != null ? balance : 0.0;
    }

    public StatsDto getStats(Long telegramId) {
        User user = userService.findByTelegramId(telegramId);
        List<Transaction> transactions = transactionRepository.findByUserOrderByCreatedAtDesc(user);
        
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        Map<String, BigDecimal> expensesByCategory = new HashMap<>();
        Map<String, BigDecimal> incomeByCategory = new HashMap<>();
        BigDecimal largestIncome = BigDecimal.ZERO;
        BigDecimal largestExpense = BigDecimal.ZERO;
        
        for (Transaction t : transactions) {
            BigDecimal amount = BigDecimal.valueOf(t.getAmount());
            
            if (t.getType() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(amount);
                incomeByCategory.merge(t.getCategory(), amount, BigDecimal::add);
                if (amount.compareTo(largestIncome) > 0) {
                    largestIncome = amount;
                }
            } else {
                totalExpense = totalExpense.add(amount);
                expensesByCategory.merge(t.getCategory(), amount, BigDecimal::add);
                if (amount.compareTo(largestExpense) > 0) {
                    largestExpense = amount;
                }
            }
        }
        
        BigDecimal balance = totalIncome.subtract(totalExpense);
        
        int transactionCount = transactions.size();
        BigDecimal averageTransaction = transactionCount > 0 
            ? balance.divide(BigDecimal.valueOf(transactionCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        return StatsDto.builder()
            .totalIncome(totalIncome)
            .totalExpense(totalExpense)
            .balance(balance)
            .expensesByCategory(expensesByCategory)
            .incomeByCategory(incomeByCategory)
            .transactionCount(transactionCount)
            .averageTransaction(averageTransaction)
            .largestIncome(largestIncome)
            .largestExpense(largestExpense)
            .build();
    }

    public List<Transaction> getUserTransactions(Long telegramId) {
        User user = userService.findByTelegramId(telegramId);
        return transactionRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public Optional<Transaction> getLastTransaction(Long telegramId) {
        User user = userService.findByTelegramId(telegramId);
        List<Transaction> transactions = transactionRepository.findTop10ByUserOrderByCreatedAtDesc(user);
        return transactions.isEmpty() ? Optional.empty() : Optional.of(transactions.get(0));
    }
    
    @Transactional
    public void deleteTransaction(Long transactionId, Long telegramId) {
        User user = userService.findByTelegramId(telegramId);
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        
        transactionRepository.delete(transaction);
        log.info("Transaction {} deleted by user {}", transactionId, telegramId);
    }
    
    @Transactional
    public void updateTransaction(Long transactionId, Long telegramId, Double amount, String category, String description) {
        User user = userService.findByTelegramId(telegramId);
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        
        if (amount != null) transaction.setAmount(amount);
        if (category != null) transaction.setCategory(category);
        if (description != null) transaction.setDescription(description);
        
        transactionRepository.save(transaction);
        log.info("Transaction {} updated by user {}", transactionId, telegramId);
    }
    
    public byte[] exportToExcel(Long telegramId) throws Exception {
        List<Transaction> transactions = getUserTransactions(telegramId);
        return exportService.exportToExcel(transactions);
    }
    
    public byte[] exportToCsv(Long telegramId) throws Exception {
        List<Transaction> transactions = getUserTransactions(telegramId);
        return exportService.exportToCsv(transactions);
    }
    
    public byte[] getExpenseChart(Long telegramId) throws Exception {
        StatsDto stats = getStats(telegramId);
        return chartService.createExpensePieChart(stats);
    }
    
    public byte[] getIncomeChart(Long telegramId) throws Exception {
        StatsDto stats = getStats(telegramId);
        return chartService.createIncomePieChart(stats);
    }
    
    public String getLastTransactionsString(Long telegramId) {
        User user = userService.findByTelegramId(telegramId);
        List<Transaction> transactions = transactionRepository.findTop10ByUserOrderByCreatedAtDesc(user);
        
        if (transactions.isEmpty()) return "📝 История операций пуста.";
        
        StringBuilder sb = new StringBuilder("📋 *Последние операции:*\n\n");
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            String sign = t.getType() == TransactionType.INCOME ? "+" : "-";
            sb.append(i + 1).append(". ")
              .append(sign).append(" ")
              .append(String.format("%.2f", t.getAmount()))
              .append(" ₽ | ")
              .append(t.getCategory())
              .append(" | ")
              .append(t.getDescription() != null ? t.getDescription() : "")
              .append("\n");
        }
        return sb.toString();
    }
    
    public String getDetailedStatsString(Long telegramId) {
        StatsDto stats = getStats(telegramId);
        
        StringBuilder sb = new StringBuilder("📊 *Детальная статистика:*\n\n");
        sb.append("💰 *Общий доход:* ").append(String.format("%.2f", stats.getTotalIncome())).append(" ₽\n");
        sb.append("💸 *Общий расход:* ").append(String.format("%.2f", stats.getTotalExpense())).append(" ₽\n");
        sb.append("⚖️ *Баланс:* ").append(String.format("%.2f", stats.getBalance())).append(" ₽\n\n");
        
        sb.append("📈 *Расходы по категориям:*\n");
        stats.getExpensesByCategory().entrySet().stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
            .forEach(e -> sb.append("  • ").append(e.getKey()).append(": ")
                .append(String.format("%.2f", e.getValue())).append(" ₽\n"));
        
        sb.append("\n📊 *Доходы по категориям:*\n");
        stats.getIncomeByCategory().entrySet().stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
            .forEach(e -> sb.append("  • ").append(e.getKey()).append(": ")
                .append(String.format("%.2f", e.getValue())).append(" ₽\n"));
        
        sb.append("\n📊 *Общая статистика:*\n");
        sb.append("  • Количество транзакций: ").append(stats.getTransactionCount()).append("\n");
        sb.append("  • Средний чек: ").append(String.format("%.2f", stats.getAverageTransaction())).append(" ₽\n");
        sb.append("  • Макс. доход: ").append(String.format("%.2f", stats.getLargestIncome())).append(" ₽\n");
        sb.append("  • Макс. расход: ").append(String.format("%.2f", stats.getLargestExpense())).append(" ₽\n");
        
        return sb.toString();
    }
}