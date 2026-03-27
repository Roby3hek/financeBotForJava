package com.roby3hek.financebot.repository;

import com.roby3hek.financebot.entity.Transaction;
import com.roby3hek.financebot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findTop10ByUserOrderByCreatedAtDesc(User user);
    List<Transaction> findByUserOrderByCreatedAtDesc(User user);
    
    @Query("SELECT COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END), 0) " +
           "FROM Transaction t WHERE t.user = :user")
    Double getBalance(User user);
}