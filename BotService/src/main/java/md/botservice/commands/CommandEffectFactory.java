package md.botservice.commands;

import lombok.RequiredArgsConstructor;
import md.botservice.exceptions.CommandNotSupportedException;
import md.botservice.models.Command;
import md.botservice.models.TelegramCommands;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CommandEffectFactory {

    private final List<CommandStrategy> strategies;

    public CommandStrategy getStrategy(Command command) {
        if (command.telegramCommand() == null) { // help by default
            return strategies.stream()
                    .filter(s -> s.supports(TelegramCommands.HELP))
                    .findFirst()
                    .orElseThrow();
        }

        return strategies.stream()
                .filter(s -> s.supports(command.telegramCommand()))
                .findFirst()
                .orElseThrow(() -> new CommandNotSupportedException("No strategy supports command: " + command));
    }
}