package md.botservice.commands;

import md.botservice.models.Command;
import md.botservice.models.Language;
import md.botservice.models.TelegramCommands;
import md.botservice.models.User;
import md.botservice.service.MessageService;
import md.botservice.service.UserService;
import md.botservice.utils.KeyboardHelper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.ArrayList;
import java.util.List;

@Component
public class MyInterestsCommandStrategy extends AbstractCommandStrategy {

    private final UserService userService;
    private final MessageService messageService;

    public MyInterestsCommandStrategy(KeyboardHelper keyboardHelper, UserService userService, MessageService messageService) {
        super(keyboardHelper);
        this.userService = userService;
        this.messageService = messageService;
    }

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.MY_INTERESTS == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        Language lang = command.user().getLanguage();

        if (command.commandParam() != null && !command.commandParam().isEmpty()) {
            userService.updateInterests(command.user().getId(), command.commandParam());
            String updatedText = messageService.get("interests.updated", lang, command.commandParam());
            sendWithMainMenu(sender, command.chatId(), updatedText, lang);
            return;
        }

        showCurrentInterestsWithButtons(sender, command.chatId(), command.user(), lang);
    }

    public void confirmKeepInterests(AbsSender sender, Long chatId, Language lang) {
        String keepText = messageService.get("interests.kept", lang);
        sendWithMainMenu(sender, chatId, keepText, lang);
    }

    private void showCurrentInterestsWithButtons(AbsSender sender, Long chatId, User user, Language lang) {
        String currentInterests = user.getInterestsRaw();

        String text = (currentInterests == null || currentInterests.trim().isEmpty())
                ? messageService.get("interests.current.empty", lang)
                : messageService.get("interests.current.exists", lang, currentInterests);

        InlineKeyboardMarkup markup = buildButtonsLayout(currentInterests, lang);
        sendMessage(sender, chatId, text, markup);
    }

    private InlineKeyboardMarkup buildButtonsLayout(String currentInterests, Language lang) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton updateBtn = new InlineKeyboardButton();
        updateBtn.setText(messageService.get("button.update_interests", lang));
        updateBtn.setCallbackData("update_interests");
        row1.add(updateBtn);
        rows.add(row1);

        if (currentInterests != null && !currentInterests.trim().isEmpty()) {
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton keepBtn = new InlineKeyboardButton();
            keepBtn.setText(messageService.get("button.keep_interests", lang));
            keepBtn.setCallbackData("keep_interests");
            row2.add(keepBtn);
            rows.add(row2);
        }

        markup.setKeyboard(rows);
        return markup;
    }
}