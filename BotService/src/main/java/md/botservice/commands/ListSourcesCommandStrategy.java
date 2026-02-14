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

        if (sources.isEmpty()) {
            sendMessage(sender, command.chatId(),
                    "📭 You have no sources.\n\nUse the button below to add one:");
            return;
        }

        // Create message with inline buttons for each source
        StringBuilder messageText = new StringBuilder("📚 *Your Sources:*\n\n");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        int index = 0;
        for (Source source : sources) {
            // Add source to message
            messageText.append(String.format("%d. `%s`\n", index + 1, source.getUrl()));

            // Create remove button for this source
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton removeBtn = new InlineKeyboardButton();
            removeBtn.setText("🗑 Remove #" + (index + 1));
            removeBtn.setCallbackData("REMOVE_SOURCE:" + source.getId());
            row.add(removeBtn);
            keyboard.add(row);

            index++;
        }

        // Add "Add new source" button
        List<InlineKeyboardButton> addRow = new ArrayList<>();
        InlineKeyboardButton addBtn = new InlineKeyboardButton();
        addBtn.setText("➕ Add New Source");
        addBtn.setCallbackData("CMD_ADD_SOURCE");
        addRow.add(addBtn);
        keyboard.add(addRow);

        markup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(command.chatId()));
        message.setText(messageText.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendMessage(AbsSender sender, Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");

        // Add "Add Source" button
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton addBtn = new InlineKeyboardButton();
        addBtn.setText("➕ Add Source");
        addBtn.setCallbackData("CMD_ADD_SOURCE");
        row.add(addBtn);
        keyboard.add(row);

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}