package md.botservice.commands;

import md.botservice.commands.CommandStrategy;
import md.botservice.model.Command;
import md.botservice.model.TelegramCommands;
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
        String welcome = """
            👋 *Hello there!*
            
            I'm ready to serve. To get started, tell me what news you want to read.
            
            Type:
            `/myinterests [your topics]`
            """;
        sendMessage(sender, command.chatId(), welcome);
    }
}