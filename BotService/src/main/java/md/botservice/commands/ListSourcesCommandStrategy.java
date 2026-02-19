package md.botservice.commands;

import md.botservice.models.Command;
import md.botservice.models.Source;
import md.botservice.models.TelegramCommands;
import md.botservice.utils.FormatUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ListSourcesCommandStrategy implements CommandStrategy {

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.LIST_SOURCES == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        Set<Source> sources = command.user().getSubscriptions();
        sources.forEach( s -> s.setUrl(FormatUtils.getSimpleTelegramName(s.getUrl())));

        if (sources.isEmpty()) {
            sendMessage(sender, command.chatId(), "📭 You have no sources.\n\nUse the button below to add one:", createAddButtonMarkup());
            return;
        }

        StringBuilder text = new StringBuilder("📚 *Your Sources:*\n\n");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        int index = 0;
        for (Source source : sources) {
            text.append(String.format("%d. `%s`\n", index + 1, source.getUrl()));

            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("🗑 Remove #" + (index + 1));
            btn.setCallbackData("REMOVE_SOURCE:" + source.getId());
            row.add(btn);
            rows.add(row);
            index++;
        }

        List<InlineKeyboardButton> addRow = new ArrayList<>();
        InlineKeyboardButton addBtn = new InlineKeyboardButton();
        addBtn.setText("➕ Add New Source");
        addBtn.setCallbackData("CMD_ADD_SOURCE");
        addRow.add(addBtn);
        rows.add(addRow);

        markup.setKeyboard(rows);

        sendMessage(sender, command.chatId(), text.toString(), markup);
    }

    private InlineKeyboardMarkup createAddButtonMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton addBtn = new InlineKeyboardButton();
        addBtn.setText("➕ Add Source");
        addBtn.setCallbackData("CMD_ADD_SOURCE");
        row.add(addBtn);
        rows.add(row);
        markup.setKeyboard(rows);
        return markup;
    }
}