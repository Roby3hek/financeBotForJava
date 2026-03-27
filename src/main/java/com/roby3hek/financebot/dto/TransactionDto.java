package com.roby3hek.financebot.dto;

import com.roby3hek.financebot.entity.TransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    @NotNull
    private Double amount;

    @NotNull
    private TransactionType type;

    @NotNull
    private String category;

    private String description;
}