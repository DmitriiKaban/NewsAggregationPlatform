package md.botservice.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NewsEventListener {

    private final TelegramBotService botService;

    public NewsEventListener(TelegramBotService botService) {
        this.botService = botService;
    }

    @KafkaListener(topics = "news.processed", groupId = "newsbot-core")
    public void consumeProcessedNews(String message) {
        // 1. Parse JSON message (Article ID, Title, Summary, Vector ID)
        // 2. Match against User Preferences (could be done here or in AI service)
        // 3. Trigger Bot Notification
//         botService.sendNewsAlert(targetChatId, title, url);
        System.out.println("Received processed news: " + message);
    }
}
