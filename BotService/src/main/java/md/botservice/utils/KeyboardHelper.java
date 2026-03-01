package md.botservice.utils;

import lombok.RequiredArgsConstructor;
import md.botservice.models.Language;
import md.botservice.service.MessageService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class KeyboardHelper {

    private final MessageService messageService;

    public ReplyKeyboardMarkup getMainMenuKeyboard(Language lang) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(messageService.get("button.my_sources", lang)));
        row1.add(new KeyboardButton(messageService.get("button.my_interests", lang)));
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton(messageService.get("button.help", lang)));
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }

    public InlineKeyboardMarkup getPostReactionKeyboard(String postId, Language lang) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> reactionRow = new ArrayList<>();

        InlineKeyboardButton likeBtn = new InlineKeyboardButton();
        likeBtn.setText(messageService.get("button.like", lang));
        likeBtn.setCallbackData("LIKE_POST:" + postId);

        InlineKeyboardButton dislikeBtn = new InlineKeyboardButton();
        dislikeBtn.setText(messageService.get("button.dislike", lang));
        dislikeBtn.setCallbackData("DISLIKE_POST:" + postId);

        reactionRow.add(likeBtn);
        reactionRow.add(dislikeBtn);

        rows.add(reactionRow);
        markup.setKeyboard(rows);

        return markup;
    }

}