package md.botservice.commands;

import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
public class StartCommandStrategy implements CommandStrategy {

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.START == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        String welcomeText = """
                👋 *Welcome to NewsBot!*
                
                I help you stay updated with personalized news from your favorite sources.
                
                *Quick Start:*
                • 🎯 Set your interests
                • 📚 Add news sources
                • 🎨 Use the Menu button (to the left of the input field)
                
                Choose an option below to get started!
                """;

        sendWithMainMenu(sender, command.chatId(), welcomeText);
    }
}