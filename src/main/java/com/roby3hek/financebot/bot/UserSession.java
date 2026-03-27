package com.roby3hek.financebot.bot;

import com.roby3hek.financebot.entity.Category;
import lombok.Data;

@Data
public class UserSession {
    private boolean isIncome;
    private boolean waitingForCategory;
    private boolean waitingForAmount;
    private boolean waitingForDescription;
    private boolean waitingForDelete;
    private double amount;
    private Category selectedCategory;
    
    public boolean isWaitingForInput() {
        return waitingForCategory || waitingForAmount || waitingForDescription || waitingForDelete;
    }
}