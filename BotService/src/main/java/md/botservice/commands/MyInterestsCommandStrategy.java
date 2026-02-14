package md.botservice.commands;

import lombok.RequiredArgsConstructor;
import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;
import md.botservice.models.User;
import md.botservice.service.UserService;
import md.botservice.utils.KeyboardHelper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MyInterestsCommandStrategy implements CommandStrategy {

    private final UserService userService;

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.MY_INTERESTS == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        // If user provided new interests as parameter
        if (command.commandParam() != null && !command.commandParam().isEmpty()) {
            userService.updateInterests(command.user().getId(), command.commandParam());

            sendSuccessMessage(sender, command.chatId(),
                    "✅ *Interests Updated!*\n\nI'll now look for news about:\n`" + command.commandParam() + "`");
            return;
        }

        // Otherwise, show current interests with action buttons
        showCurrentInterestsWithButtons(sender, command.chatId(), command.user());
    }

    private void showCurrentInterestsWithButtons(AbsSender sender, Long chatId, User user) {
        String currentInterests = user.getInterestsRaw();

        String messageText;
        if (currentInterests == null || currentInterests.trim().isEmpty()) {
            messageText = "🎯 *Your Interests*\n\nYou haven't set any interests yet.\n\nWhat would you like to do?";
        } else {
            messageText = "🎯 *Your Current Interests:*\n\n`" + currentInterests + "`\n\nWhat would you like to do?";
        }

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(messageText);
        message.setParseMode("Markdown");

        // Create inline keyboard with action buttons
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Row 1: Update Interests button
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton updateBtn = new InlineKeyboardButton();
        updateBtn.setText("✏️ Update Interests");
        updateBtn.setCallbackData("update_interests");
        row1.add(updateBtn);
        keyboard.add(row1);

        // Row 2: Keep as is button (only if interests exist)
        if (currentInterests != null && !currentInterests.trim().isEmpty()) {
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton keepBtn = new InlineKeyboardButton();
            keepBtn.setText("✅ Keep as is");
            keepBtn.setCallbackData("keep_interests");
            row2.add(keepBtn);
            keyboard.add(row2);
        }

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendSuccessMessage(AbsSender sender, Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(KeyboardHelper.getMainMenuKeyboard()); // Restore buttons

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void promptForNewInterests(AbsSender sender, Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("🎯 *Enter Your New Interests*\n\nReply to this message with keywords separated by commas.\n\n_Example: AI, Politics MD, Formula 1_");
        message.setParseMode("Markdown");
        message.setReplyMarkup(new ForceReplyKeyboard(true));

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void confirmKeepInterests(AbsSender sender, Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("✅ *Interests Kept*\n\nYour interests remain unchanged.");
        message.setParseMode("Markdown");
        message.setReplyMarkup(KeyboardHelper.getMainMenuKeyboard());

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}