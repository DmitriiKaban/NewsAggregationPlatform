package md.botservice.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.events.SummaryNotificationEvent;
import md.botservice.models.Language;
import md.botservice.models.User;
import md.botservice.repository.UserRepository;
import md.botservice.service.MessageService;
import md.botservice.service.TelegramBotService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class SummaryEventListener {

    private final TelegramBotService telegramBotService;
    private final MessageService messageService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "user-summaries",
            groupId = "bot-service-summary-group-v4",
            properties = {"auto.offset.reset=earliest"}
    )
    public void consumeSummary(String messagePayload) {
        try {
            SummaryNotificationEvent event = objectMapper.readValue(messagePayload, SummaryNotificationEvent.class);

            User user = userRepository.findById(event.userId()).orElse(null);
            if (user == null || event.articles() == null || event.articles().isEmpty()) {
                return;
            }

            Language lang = user.getLanguage();
            String titleKey = "DAILY".equals(event.type()) ? "summary.daily.title" : "summary.weekly.title";

            StringBuilder sb = new StringBuilder();
            sb.append("🤖 <b>").append(messageService.get(titleKey, lang)).append("</b>\n\n");

            for (var article : event.articles()) {
                sb.append("🔹 <b>").append(article.title()).append("</b> (<i>").append(article.sourceName()).append("</i>)\n");

                if (article.summary() != null && !article.summary().isBlank() && !"null".equals(article.summary())) {
                    String text = article.summary().length() > 150 ? article.summary().substring(0, 150) + "..." : article.summary();
                    sb.append(text).append("\n");
                }
                sb.append("<a href='").append(article.url()).append("'>").append(messageService.get("summary.read_more", lang)).append("</a>\n\n");
            }

            sb.append("<i>").append(messageService.get("summary.footer", lang)).append("</i>");

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(user.getId()));
            message.setText(sb.toString());
            message.setParseMode("HTML");

            telegramBotService.execute(message);
            log.info("Successfully sent localized AI summary to user {}", user.getId());

        } catch (Exception e) {
            log.error("Failed to process and send localized AI summary", e);
        }
    }

}