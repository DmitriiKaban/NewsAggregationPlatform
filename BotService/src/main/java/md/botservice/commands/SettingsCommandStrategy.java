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

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton strictModeBtn = new InlineKeyboardButton();

        String statusKey = user.isShowOnlySubscribedSources() ? "settings.status_on" : "settings.status_off";
        String statusText = messageService.get(statusKey, lang);

        strictModeBtn.setText(messageService.get("settings.strict_filter", lang, statusText));
        strictModeBtn.setCallbackData("TOGGLE_STRICT_MODE");

        row1.add(strictModeBtn);
        rows.add(row1);

        markup.setKeyboard(rows);

        sendMessage(sender, command.chatId(), text, markup);
    }
}