package md.botservice.commands;

import md.botservice.models.Command;
import md.botservice.models.Language;
import md.botservice.models.Source;
import md.botservice.models.TelegramCommands;
import md.botservice.service.MessageService;
import md.botservice.utils.FormatUtils;
import md.botservice.utils.KeyboardHelper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
public class ListSourcesCommandStrategy extends AbstractCommandStrategy {

    private final MessageService messageService;

    public ListSourcesCommandStrategy(KeyboardHelper keyboardHelper, MessageService messageService) {
        super(keyboardHelper);
        this.messageService = messageService;
    }

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.LIST_SOURCES == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        Language lang = command.user().getLanguage();
        Set<Source> sourcesSet = command.user().getSubscriptions();

        if (sourcesSet.isEmpty()) {
            String emptyText = messageService.get("sources.empty", lang);
            sendMessage(sender, command.chatId(), emptyText, createAddButtonMarkup(lang));
            return;
        }

        List<Source> sources = new ArrayList<>(sourcesSet);
        sources.sort(Comparator.comparing(Source::getId));

        StringBuilder text = new StringBuilder(messageService.get("sources.list_title", lang));
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        int index = 0;
        for (Source source : sources) {
            String simpleName = FormatUtils.getSimpleTelegramName(source.getUrl());
            text.append(String.format("%d. `%s`\n", index + 1, simpleName));

            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton btn = new InlineKeyboardButton();

            btn.setText(messageService.get("button.remove_source", lang, index + 1));
            btn.setCallbackData("REMOVE_SOURCE:" + source.getId());

            row.add(btn);
            rows.add(row);
            index++;
        }

        List<InlineKeyboardButton> addRow = new ArrayList<>();
        InlineKeyboardButton addBtn = new InlineKeyboardButton();
        addBtn.setText(messageService.get("button.add_new_source", lang));
        addBtn.setCallbackData("CMD_ADD_SOURCE");
        addRow.add(addBtn);
        rows.add(addRow);

        markup.setKeyboard(rows);

        sendMessage(sender, command.chatId(), text.toString(), markup);
    }

    private InlineKeyboardMarkup createAddButtonMarkup(Language lang) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton addBtn = new InlineKeyboardButton();
        addBtn.setText(messageService.get("button.add_source", lang));
        addBtn.setCallbackData("CMD_ADD_SOURCE");

        row.add(addBtn);
        rows.add(row);
        markup.setKeyboard(rows);

        return markup;
    }
}