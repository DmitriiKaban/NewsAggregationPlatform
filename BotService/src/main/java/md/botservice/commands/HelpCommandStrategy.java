package md.botservice.commands;

import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;
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
            
            📌 *Basics:*
            /start - Begin your journey
            /help - Show this menu
            
            🎯 *Personalization:*
            /myinterests `[topics]`
            Tell me what you love! I will hunt for news matching these keywords.
            
            📚 *Manage Sources:*
            /addsource `[link]` - Subscribe to a Telegram channel or RSS feed
            /removesource `[link]` - Unsubscribe from a source
            /sources - See your active subscriptions
            
            ━━━━━━━━━━━━━━━━
            *💡 Examples:*
            `/myinterests Crypto, SpaceX, Java 21`
            `/addsource https://t.me/jolybells`
            """;

        sendMessage(sender, command.chatId(), helpText);
    }
}