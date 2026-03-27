package com.roby3hek.financebot.service;

import com.roby3hek.financebot.entity.Category;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Service
public class BotKeyboardService {
    
    public ReplyKeyboardMarkup getMainMenu() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("💰 Баланс"));
        row1.add(new KeyboardButton("📊 Статистика"));
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("➕ Добавить доход"));
        row2.add(new KeyboardButton("➖ Добавить расход"));
        
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("📋 История"));
        row3.add(new KeyboardButton("🗑 Удалить последнюю"));
        
        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("📈 График расходов"));
        row4.add(new KeyboardButton("📈 График доходов"));
        
        KeyboardRow row5 = new KeyboardRow();
        row5.add(new KeyboardButton("📤 Экспорт Excel"));
        row5.add(new KeyboardButton("📤 Экспорт CSV"));
        
        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);
        keyboard.add(row5);
        
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(keyboard);
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false);
        
        return markup;
    }
    
    public InlineKeyboardMarkup getCategoryKeyboard(boolean isIncome) {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        int count = 0;
        
        for (Category category : Category.values()) {
            if (category.isIncome() == isIncome) {
                if (count == 2) {
                    keyboard.add(row);
                    row = new InlineKeyboardRow();
                    count = 0;
                }
                InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(category.getDisplayName())
                    .callbackData("cat_" + category.name())
                    .build();
                row.add(button);
                count++;
            }
        }
        
        if (!row.isEmpty()) {
            keyboard.add(row);
        }
        
        // Кнопка отмены
        InlineKeyboardRow cancelRow = new InlineKeyboardRow();
        InlineKeyboardButton cancelButton = InlineKeyboardButton.builder()
            .text("❌ Отмена")
            .callbackData("cancel")
            .build();
        cancelRow.add(cancelButton);
        keyboard.add(cancelRow);
        
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
    
    public InlineKeyboardMarkup getConfirmKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        InlineKeyboardRow row1 = new InlineKeyboardRow();
        InlineKeyboardButton confirm = InlineKeyboardButton.builder()
            .text("✅ Да, удалить")
            .callbackData("confirm_delete")
            .build();
        row1.add(confirm);
        
        InlineKeyboardRow row2 = new InlineKeyboardRow();
        InlineKeyboardButton cancel = InlineKeyboardButton.builder()
            .text("❌ Нет, отменить")
            .callbackData("cancel")
            .build();
        row2.add(cancel);
        
        keyboard.add(row1);
        keyboard.add(row2);
        
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}