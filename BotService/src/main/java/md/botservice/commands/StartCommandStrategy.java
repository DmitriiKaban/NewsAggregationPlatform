package md.botservice.commands;

import md.botservice.models.Command;
import md.botservice.models.Language;
import md.botservice.models.TelegramCommands;
import md.botservice.service.MessageService;
import md.botservice.utils.KeyboardHelper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class StartCommandStrategy extends AbstractCommandStrategy {

    private final MessageService messageService;

    public StartCommandStrategy(KeyboardHelper keyboardHelper, MessageService messageService) {
        super(keyboardHelper);
        this.messageService = messageService;
    }

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.START == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        Language lang = command.user().getLanguage();

        if (lang == null) {
            sendLanguageSelection(command.chatId(), sender);
            return;
        }

        String welcomeText = messageService.get("welcome.full_text", lang);
        sendWithMainMenu(sender, command.chatId(), welcomeText, lang);
    }

    private void sendLanguageSelection(Long chatId, AbsSender sender) {
        String text = """
                👋 Welcome to NewsBot!
                Bun venit la NewsBot!
                Добро пожаловать в NewsBot!
                
                Please choose your preferred language:
                Vă rugăm să alegeți limba preferată:
                Пожалуйста, выберите предпочитаемый язык:
                """;

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(getLanguageKeyboard());

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send language selection", e);
        }
    }

    private InlineKeyboardMarkup getLanguageKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton englishBtn = new InlineKeyboardButton();
        englishBtn.setText("🇬🇧 English");
        englishBtn.setCallbackData("LANG_EN");
        row1.add(englishBtn);
        keyboard.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton romanianBtn = new InlineKeyboardButton();
        romanianBtn.setText("🇷🇴 Română");
        romanianBtn.setCallbackData("LANG_RO");
        row2.add(romanianBtn);
        keyboard.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton russianBtn = new InlineKeyboardButton();
        russianBtn.setText("🇷🇺 Русский");
        russianBtn.setCallbackData("LANG_RU");
        row3.add(russianBtn);
        keyboard.add(row3);

        markup.setKeyboard(keyboard);
        return markup;
    }
}