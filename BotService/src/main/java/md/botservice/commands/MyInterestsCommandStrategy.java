package md.botservice.commands;

import lombok.RequiredArgsConstructor;
import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;
import md.botservice.service.UserService;
import md.botservice.utils.KeyboardHelper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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

            sendSuccessMessage(sender, command.chatId(),
                    "✅ *Interests Updated!* \nI'll look for news about: " + command.commandParam());
            return;
        }

        sendForceReply(sender, command.chatId(),
                "🎯 *What are you interested in?*\n\nReply to this message with keywords separated by commas.\n_Example: AI, Politics MD, Formula 1_");
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

    private void sendForceReply(AbsSender sender, Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(new ForceReplyKeyboard(true)); // Force user to reply

        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}