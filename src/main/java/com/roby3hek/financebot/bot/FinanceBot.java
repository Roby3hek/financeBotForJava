package com.roby3hek.financebot.bot;

import com.roby3hek.financebot.entity.Category;
import com.roby3hek.financebot.entity.Transaction;
import com.roby3hek.financebot.entity.TransactionType;
import com.roby3hek.financebot.entity.User;
import com.roby3hek.financebot.service.BotKeyboardService;
import com.roby3hek.financebot.service.TransactionService;
import com.roby3hek.financebot.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class FinanceBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final UserService userService;
    private final TransactionService transactionService;
    private final BotKeyboardService keyboardService;
    private final TelegramClient telegramClient;
    
    private final Map<Long, UserSession> userSessions = new HashMap<>();

    public FinanceBot(UserService userService, TransactionService transactionService, BotKeyboardService keyboardService) {
        this.userService = userService;
        this.transactionService = transactionService;
        this.keyboardService = keyboardService;
        this.telegramClient = new OkHttpTelegramClient(getBotToken());
    }

    @Override
    public String getBotToken() {
        String token = System.getenv("TELEGRAM_BOT_TOKEN");
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("TELEGRAM_BOT_TOKEN environment variable is not set");
        }
        return token;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallback(update);
        }
    }
    
    private void handleMessage(Update update) {
        String text = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();
        String username = update.getMessage().getFrom().getUserName();
        
        User user = userService.findOrCreate(chatId, username);
        UserSession session = userSessions.get(chatId);
        
        try {
            if (session != null && session.isWaitingForInput()) {
                handleSessionInput(chatId, user, text);
                return;
            }
            
            switch (text) {
                case "/start":
                case "🏠 Главное меню":
                    startCommand(chatId);
                    break;
                case "/balance":
                case "💰 Баланс":
                    balanceCommand(chatId, user);
                    break;
                case "/stats":
                case "📊 Статистика":
                    statsCommand(chatId, user);
                    break;
                case "➕ Добавить доход":
                    startAddTransaction(chatId, true);
                    break;
                case "➖ Добавить расход":
                    startAddTransaction(chatId, false);
                    break;
                case "/history":
                case "📋 История":
                    historyCommand(chatId, user);
                    break;
                case "/delete":
                case "🗑 Удалить последнюю":
                    deleteLastCommand(chatId, user);
                    break;
                case "/expense_chart":
                case "📈 График расходов":
                    expenseChartCommand(chatId, user);
                    break;
                case "/income_chart":
                case "📈 График доходов":
                    incomeChartCommand(chatId, user);
                    break;
                case "/export_excel":
                case "📤 Экспорт Excel":
                    exportExcelCommand(chatId, user);
                    break;
                case "/export_csv":
                case "📤 Экспорт CSV":
                    exportCsvCommand(chatId, user);
                    break;
                default:
                    unknownCommand(chatId);
            }
        } catch (Exception e) {
            log.error("Error handling message", e);
            sendMessage(chatId, "❌ Произошла ошибка. Попробуйте позже.");
        }
    }
    
    private void handleCallback(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        User user = userService.findOrCreate(chatId, update.getCallbackQuery().getFrom().getUserName());
        UserSession session = userSessions.get(chatId);
        
        try {
            if (callbackData.equals("cancel")) {
                userSessions.remove(chatId);
                sendMessage(chatId, "❌ Действие отменено", keyboardService.getMainMenu());
                return;
            }
            
            if (callbackData.equals("confirm_delete") && session != null && session.isWaitingForDelete()) {
                Optional<Transaction> lastTransaction = transactionService.getLastTransaction(user.getTelegramId());
                if (lastTransaction.isPresent()) {
                    transactionService.deleteTransaction(lastTransaction.get().getId(), user.getTelegramId());
                    sendMessage(chatId, "✅ Последняя транзакция удалена!", keyboardService.getMainMenu());
                } else {
                    sendMessage(chatId, "📝 Нет транзакций для удаления", keyboardService.getMainMenu());
                }
                userSessions.remove(chatId);
                return;
            }
            
            if (callbackData.startsWith("cat_") && session != null && session.isWaitingForCategory()) {
                String categoryName = callbackData.substring(4);
                Category category = Category.valueOf(categoryName);
                session.setSelectedCategory(category);
                session.setWaitingForCategory(false);
                session.setWaitingForAmount(true);
                sendMessage(chatId, "💰 Введите сумму (например: 1000):");
                return;
            }
            
        } catch (Exception e) {
            log.error("Error handling callback", e);
            sendMessage(chatId, "❌ Произошла ошибка");
            userSessions.remove(chatId);
        }
    }
    
    private void handleSessionInput(long chatId, User user, String text) {
        UserSession session = userSessions.get(chatId);
        
        if (session.isWaitingForAmount()) {
            try {
                double amount = Double.parseDouble(text);
                session.setAmount(amount);
                session.setWaitingForAmount(false);
                session.setWaitingForDescription(true);
                sendMessage(chatId, "📝 Введите описание (или '-' для пропуска):");
            } catch (NumberFormatException e) {
                sendMessage(chatId, "❌ Неверный формат суммы. Введите число:");
            }
        } else if (session.isWaitingForDescription()) {
            String description = text.equals("-") ? "" : text;
            
            transactionService.addTransaction(
                user.getTelegramId(),
                session.getAmount(),
                session.isIncome() ? "INCOME" : "EXPENSE",
                session.getSelectedCategory().getDisplayName(),
                description
            );
            
            String typeStr = session.isIncome() ? "доход" : "расход";
            sendMessage(chatId, String.format("✅ %s добавлен!\nСумма: %.2f ₽\nКатегория: %s\nОписание: %s",
                typeStr, session.getAmount(), session.getSelectedCategory().getDisplayName(), 
                description.isEmpty() ? "-" : description), keyboardService.getMainMenu());
            
            userSessions.remove(chatId);
        }
    }
    
    private void startCommand(long chatId) {
        sendMessage(chatId, 
            "🤖 *Привет! Я твой финансовый помощник*\n\n" +
            "Я помогу тебе:\n" +
            "💰 Отслеживать доходы и расходы\n" +
            "📊 Анализировать статистику\n" +
            "📈 Строить графики\n" +
            "📤 Экспортировать данные\n\n" +
            "Используй кнопки ниже для управления!",
            keyboardService.getMainMenu());
    }
    
    private void startAddTransaction(long chatId, boolean isIncome) {
        UserSession session = new UserSession();
        session.setIncome(isIncome);
        session.setWaitingForCategory(true);
        session.setWaitingForAmount(false);
        session.setWaitingForDescription(false);
        userSessions.put(chatId, session);
        
        String text = isIncome ? "Выберите категорию дохода:" : "Выберите категорию расхода:";
        sendMessageWithInlineKeyboard(chatId, text, keyboardService.getCategoryKeyboard(isIncome));
    }
    
    private void balanceCommand(long chatId, User user) {
        double balance = transactionService.getBalance(user.getTelegramId());
        sendMessage(chatId, String.format("💰 *Ваш баланс:* %.2f ₽", balance));
    }
    
    private void statsCommand(long chatId, User user) {
        String stats = transactionService.getDetailedStatsString(user.getTelegramId());
        sendMessage(chatId, stats);
    }
    
    private void historyCommand(long chatId, User user) {
        String history = transactionService.getLastTransactionsString(user.getTelegramId());
        sendMessage(chatId, history);
    }
    
    private void deleteLastCommand(long chatId, User user) {
        Optional<Transaction> lastTransaction = transactionService.getLastTransaction(user.getTelegramId());
        if (lastTransaction.isPresent()) {
            Transaction t = lastTransaction.get();
            String message = String.format(
                "⚠️ *Удалить последнюю транзакцию?*\n\n" +
                "Тип: %s\n" +
                "Сумма: %.2f ₽\n" +
                "Категория: %s\n" +
                "Описание: %s",
                t.getType() == TransactionType.INCOME ? "💰 Доход" : "💸 Расход",
                t.getAmount(),
                t.getCategory(),
                t.getDescription() != null ? t.getDescription() : "-"
            );
            
            UserSession session = new UserSession();
            session.setWaitingForDelete(true);
            userSessions.put(chatId, session);
            
            sendMessageWithInlineKeyboard(chatId, message, keyboardService.getConfirmKeyboard());
        } else {
            sendMessage(chatId, "📝 Нет транзакций для удаления");
        }
    }
    
    private void expenseChartCommand(long chatId, User user) {
        try {
            byte[] chart = transactionService.getExpenseChart(user.getTelegramId());
            sendPhoto(chatId, chart, "📊 Расходы по категориям");
        } catch (Exception e) {
            log.error("Error creating expense chart", e);
            sendMessage(chatId, "❌ Не удалось создать график. Возможно нет данных о расходах.");
        }
    }
    
    private void incomeChartCommand(long chatId, User user) {
        try {
            byte[] chart = transactionService.getIncomeChart(user.getTelegramId());
            sendPhoto(chatId, chart, "📊 Доходы по категориям");
        } catch (Exception e) {
            log.error("Error creating income chart", e);
            sendMessage(chatId, "❌ Не удалось создать график. Возможно нет данных о доходах.");
        }
    }
    
    private void exportExcelCommand(long chatId, User user) {
        try {
            byte[] excel = transactionService.exportToExcel(user.getTelegramId());
            sendDocument(chatId, excel, "transactions.xlsx", "📊 Экспорт транзакций в Excel");
        } catch (Exception e) {
            log.error("Error exporting to Excel", e);
            sendMessage(chatId, "❌ Не удалось экспортировать данные");
        }
    }
    
    private void exportCsvCommand(long chatId, User user) {
        try {
            byte[] csv = transactionService.exportToCsv(user.getTelegramId());
            sendDocument(chatId, csv, "transactions.csv", "📊 Экспорт транзакций в CSV");
        } catch (Exception e) {
            log.error("Error exporting to CSV", e);
            sendMessage(chatId, "❌ Не удалось экспортировать данные");
        }
    }
    
    private void unknownCommand(long chatId) {
        sendMessage(chatId, "❓ Неизвестная команда. Используйте кнопки меню!", keyboardService.getMainMenu());
    }
    
    private void sendMessage(long chatId, String text) {
        sendMessage(chatId, text, null);
    }
    
    private void sendMessage(long chatId, String text, ReplyKeyboard keyboard) {
        try {
            SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(keyboard)
                .build();
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message", e);
        }
    }
    
    private void sendMessageWithInlineKeyboard(long chatId, String text, InlineKeyboardMarkup keyboard) {
        try {
            SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(keyboard)
                .build();
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message with inline keyboard", e);
        }
    }
    
    private void sendPhoto(long chatId, byte[] photo, String caption) {
        try {
            SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(new ByteArrayInputStream(photo), "chart.png"))
                .caption(caption)
                .build();
            telegramClient.execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error("Error sending photo", e);
        }
    }
    
    private void sendDocument(long chatId, byte[] document, String filename, String caption) {
        try {
            SendDocument sendDocument = SendDocument.builder()
                .chatId(chatId)
                .document(new InputFile(new ByteArrayInputStream(document), filename))
                .caption(caption)
                .build();
            telegramClient.execute(sendDocument);
        } catch (TelegramApiException e) {
            log.error("Error sending document", e);
        }
    }
}