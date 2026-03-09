package md.botservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import md.botservice.events.NewsNotificationEvent;
import md.botservice.models.User;
import md.botservice.utils.KeyboardHelper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Slf4j
@Service
public class NewsEventListener {

    private final TelegramBotService botService;
    private final KeyboardHelper keyboardHelper;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NewsEventListener(TelegramBotService botService, KeyboardHelper keyboardHelper, UserService userService) {
        this.botService = botService;
        this.keyboardHelper = keyboardHelper;
        this.userService = userService;
    }

    @KafkaListener(topics = "news.notification", groupId = "newsbot-notification-group")
    public void consumeNotification(NewsNotificationEvent event) {
        try {
            User user = userService.findById(event.userId());

            InlineKeyboardMarkup reactionKeyboard = keyboardHelper.getPostReactionKeyboard(event.postId(), user.getLanguage());

            botService.sendNewsAlert(event, reactionKeyboard);

        } catch (Exception e) {
            log.error("Error sending notification for user {}: {}", event.userId(), e.getMessage());
        }
    }

}