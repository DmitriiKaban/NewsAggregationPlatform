package md.botservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.models.User;
import md.botservice.repository.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSummaryScheduler {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String SUMMARY_REQUEST_TOPIC = "summary.generate.request";

    @Scheduled(cron = "0 0 20 * * *")
    public void requestDailySummaries() {
        log.info("Triggering Daily AI Summaries...");
        List<User> users = userRepository.findByDailySummaryEnabledTrue();
        users.forEach(u -> sendRequest(u.getId(), "DAILY"));
    }

    @Scheduled(cron = "0 0 20 * * SUN")
    public void requestWeeklySummaries() {
        log.info("Triggering Weekly AI Summaries...");
        List<User> users = userRepository.findByWeeklySummaryEnabledTrue();
        users.forEach(u -> sendRequest(u.getId(), "WEEKLY"));
    }

    private void sendRequest(Long userId, String type) {
        String payload = String.format("{\"userId\": %d, \"type\": \"%s\"}", userId, type);
        kafkaTemplate.send(SUMMARY_REQUEST_TOPIC, String.valueOf(userId), payload);
        log.debug("Sent summary request for User: {} Type: {}", userId, type);
    }

}