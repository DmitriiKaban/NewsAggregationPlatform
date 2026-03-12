package md.botservice.commands;

import md.botservice.models.Command;
import md.botservice.models.Language;
import md.botservice.models.TelegramCommands;
import md.botservice.models.User;
import md.botservice.service.MessageService;
import md.botservice.utils.KeyboardHelper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.ArrayList;
import java.util.List;

@Component
public class SettingsCommandStrategy extends AbstractCommandStrategy {

    private final MessageService messageService;

    public SettingsCommandStrategy(KeyboardHelper keyboardHelper, MessageService messageService) {
        super(keyboardHelper);
        this.messageService = messageService;
    }

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.SETTINGS == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        User user = command.user();
        Language lang = user.getLanguage();

        String text = messageService.get("settings.title_desc", lang);
        InlineKeyboardMarkup markup = buildSettingsKeyboard(user, lang, messageService);
        sendMessage(sender, command.chatId(), text, markup);
    }

    public static InlineKeyboardMarkup buildSettingsKeyboard(User user, Language lang, MessageService messageService) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton strictModeBtn = new InlineKeyboardButton();
        String strictStatus = user.isShowOnlySubscribedSources() ? "✅ " : "❌ ";
        strictModeBtn.setText(strictStatus + messageService.get("settings.strict_filter", lang));
        strictModeBtn.setCallbackData("TOGGLE_STRICT_MODE");
        row1.add(strictModeBtn);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton dailyBtn = new InlineKeyboardButton();
        String dailyStatus = user.isDailySummaryEnabled() ? "✅ " : "❌ ";
        dailyBtn.setText(dailyStatus + "Daily AI Summary");
        dailyBtn.setCallbackData("TOGGLE_DAILY_SUMMARY");
        row2.add(dailyBtn);
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton weeklyBtn = new InlineKeyboardButton();
        String weeklyStatus = user.isWeeklySummaryEnabled() ? "✅ " : "❌ ";
        weeklyBtn.setText(weeklyStatus + "Weekly AI Summary");
        weeklyBtn.setCallbackData("TOGGLE_WEEKLY_SUMMARY");
        row3.add(weeklyBtn);
        rows.add(row3);

        markup.setKeyboard(rows);
        return markup;
    }

}