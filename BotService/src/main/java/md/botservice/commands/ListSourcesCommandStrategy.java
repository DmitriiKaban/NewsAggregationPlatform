package md.botservice.commands;

import md.botservice.models.Command;
import md.botservice.models.Source;
import md.botservice.models.TelegramCommands;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class ListSourcesCommandStrategy implements CommandStrategy {

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.LIST_SOURCES == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        Set<Source> sources = command.user().getSubscriptions();

        StringBuilder text = new StringBuilder("📚 *Your Subscriptions:*\n\n");
        if (sources.isEmpty()) {
            text.append("_(No sources yet)_\n");
        } else {
            for (Source s : sources) {
                text.append("• ").append(s.getName()).append("\n");
            }
        }
        text.append("\n_Manage your sources below:_");

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(command.chatId()));
        message.setText(text.toString());
        message.setParseMode("Markdown");

        // 1. Create INLINE buttons (Action buttons inside the chat)
        InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();

        InlineKeyboardButton addBtn = new InlineKeyboardButton();
        addBtn.setText("➕ Add Source");
        addBtn.setCallbackData("CMD_ADD_SOURCE");
        row1.add(addBtn);

        InlineKeyboardButton rmvBtn = new InlineKeyboardButton();
        rmvBtn.setText("➖ Remove Source");
        rmvBtn.setCallbackData("CMD_REMOVE_SOURCE");
        row1.add(rmvBtn);

        rows.add(row1);
        inlineMarkup.setKeyboard(rows);

        message.setReplyMarkup(inlineMarkup);

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}