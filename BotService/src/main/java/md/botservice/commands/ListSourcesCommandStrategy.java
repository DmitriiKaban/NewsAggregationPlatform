package md.botservice.commands;

import md.botservice.model.Command;
import md.botservice.model.Source;
import md.botservice.model.TelegramCommands;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;

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

        if (sources.isEmpty()) {
            sendMessage(sender, command.chatId(), "📭 У вас нет подписок на источники.\nИспользуйте `/addsource [ссылка]`");
            return;
        }

        String list = sources.stream()
                .map(s -> String.format("• [%s](%s) (%s)", s.getName(), s.getUrl(), s.getType()))
                .collect(Collectors.joining("\n"));

        sendMessage(sender, command.chatId(), "📚 *Ваши источники:*\n\n" + list);
    }
}
