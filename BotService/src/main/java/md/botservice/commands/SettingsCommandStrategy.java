package md.botservice.commands;

import lombok.RequiredArgsConstructor;
import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;
import md.botservice.models.User;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SettingsCommandStrategy implements CommandStrategy {

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.SETTINGS == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        User user = command.user();

        String text = "⚙️ *News Settings*\n\n" +
                "Customize how your news feed behaves.";

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton strictModeBtn = new InlineKeyboardButton();

        String status = user.isShowOnlySubscribedSources() ? "✅ ON" : "❌ OFF";
        strictModeBtn.setText("Strict Filter (Only My Sources): " + status);
        strictModeBtn.setCallbackData("TOGGLE_STRICT_MODE");

        row1.add(strictModeBtn);
        rows.add(row1);

        markup.setKeyboard(rows);

        sendMessage(sender, command.chatId(), text, markup);
    }
}
