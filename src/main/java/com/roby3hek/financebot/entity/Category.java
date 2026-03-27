package com.roby3hek.financebot.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Category {
    // Доходы
    SALARY("💰 Зарплата", true),
    FREELANCE("💻 Фриланс", true),
    INVESTMENT("📈 Инвестиции", true),
    GIFT("🎁 Подарок", true),
    OTHER_INCOME("📌 Другой доход", true),
    
    // Расходы
    FOOD("🍔 Еда", false),
    TRANSPORT("🚗 Транспорт", false),
    ENTERTAINMENT("🎮 Развлечения", false),
    SHOPPING("🛍 Покупки", false),
    UTILITIES("💡 Коммунальные", false),
    HEALTH("🏥 Здоровье", false),
    EDUCATION("📚 Образование", false),
    OTHER_EXPENSE("📌 Другие расходы", false);
    
    private final String displayName;
    private final boolean isIncome;
    
    public static Category fromDisplayName(String displayName) {
        for (Category category : values()) {
            if (category.displayName.equals(displayName)) {
                return category;
            }
        }
        return null;
    }
}