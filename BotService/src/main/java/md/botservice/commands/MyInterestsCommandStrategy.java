package md.botservice.commands;

import lombok.RequiredArgsConstructor;
import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;
import md.botservice.models.User;
import md.botservice.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

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
        if (command.commandParam() != null && !command.commandParam().isEmpty()) {
            userService.updateInterests(command.user().getId(), command.commandParam());

            sendWithMainMenu(sender, command.chatId(),
                    "✅ *Interests Updated!*\n\nI'll now look for news about:\n`" + command.commandParam() + "`");
            return;
        }

        showCurrentInterestsWithButtons(sender, command.chatId(), command.user());
    }

    public void confirmKeepInterests(AbsSender sender, Long chatId) {
        sendWithMainMenu(sender, chatId, "✅ *Interests Kept*\n\nYour interests remain unchanged.");
    }

    private void showCurrentInterestsWithButtons(AbsSender sender, Long chatId, User user) {
        String currentInterests = user.getInterestsRaw();

        String text = (currentInterests == null || currentInterests.trim().isEmpty())
                ? "🎯 *Your Interests*\n\nYou haven't set any interests yet.\n\nWhat would you like to do?"
                : "🎯 *Your Current Interests:*\n\n`" + currentInterests + "`\n\nWhat would you like to do?";

        InlineKeyboardMarkup markup = buildButtonsLayout(currentInterests);

        sendMessage(sender, chatId, text, markup);
    }

    private static InlineKeyboardMarkup buildButtonsLayout(String currentInterests) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Row 1: Update
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton updateBtn = new InlineKeyboardButton();
        updateBtn.setText("✏️ Update Interests");
        updateBtn.setCallbackData("update_interests");
        row1.add(updateBtn);
        rows.add(row1);

        // Row 2: Keep (only if interests exist)
        if (currentInterests != null && !currentInterests.trim().isEmpty()) {
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton keepBtn = new InlineKeyboardButton();
            keepBtn.setText("✅ Keep as is");
            keepBtn.setCallbackData("keep_interests");
            row2.add(keepBtn);
            rows.add(row2);
        }
        markup.setKeyboard(rows);
        return markup;
    }
}