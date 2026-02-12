package md.botservice.commands;

import md.botservice.model.Command;
import md.botservice.model.TelegramCommands;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
public class HelpCommandStrategy implements CommandStrategy {

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.HELP == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        String helpText = """
            ✨ *Welcome to NewsBot!* ✨
            
            I am your personal AI news curator. I filter out the noise and deliver only what you care about.
            
            *Here is how to control me:*
            
            📌 */start*
            Begin your journey and register.
            
            🎯 */myinterests* `[topics]`
            Tell me what you love! I will hunt for news matching these keywords.
            
            ❓ */help*
            Show this menu again.
            
            ━━━━━━━━━━━━━━━━
            *💡 Try it now:*
            `/myinterests Crypto, SpaceX, Java 21`
            """;

        sendMessage(sender, command.chatId(), helpText);
    }
}