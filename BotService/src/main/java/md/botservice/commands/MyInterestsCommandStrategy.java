package md.botservice.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.model.Command;
import md.botservice.model.TelegramCommands;
import md.botservice.model.User;
import md.botservice.service.UserService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.bots.AbsSender;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyInterestsCommandStrategy implements CommandStrategy {

    private final UserService userService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(TelegramCommands command) {
        return TelegramCommands.MY_INTERESTS == command;
    }

    @Override
    public void execute(Command command, AbsSender sender) {
        String interests = command.commandParam();
        User user = command.user();

        if (interests == null || interests.trim().isEmpty()) {
            sendMessage(sender, command.chatId(),
                    "⚠️ *Missing Topics*\n\nPlease type your interests after the command.\n\n*Example:*\n`/myinterests Stock Market, AI, Formula 1`");
            return;
        }

        user.setInterestsRaw(interests);
        userService.updateUser(user);

        try {
            Map<String, Object> event = new HashMap<>();
            event.put("userId", user.getId());
            event.put("interests", interests);

            String jsonEvent = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("user.interests.updated", jsonEvent);

            sendMessage(sender, command.chatId(),
                    "✅ *Interests Saved!*\n\nI will now look for news about:\n`" + interests + "`");

            log.info("Updated interests for user: {}", user.getId());

        } catch (Exception e) {
            log.error("Failed to send Kafka event for user {}", user.getId(), e);
            sendMessage(sender, command.chatId(), "❌ *System Error*\n\nCould not save interests. Please try again later.");
        }
    }
}