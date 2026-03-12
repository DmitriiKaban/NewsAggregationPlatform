package md.botservice.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.dto.AnalyticsEventDto;
import md.botservice.models.AnalyticsEvent;
import md.botservice.models.User;
import md.botservice.repository.AnalyticsEventRepository;
import md.botservice.repository.UserRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsConsumer {

    private final AnalyticsEventRepository analyticsEventRepository;
    private final UserRepository userRepository;

    @KafkaListener(
            topics = "bot.analytics.events",
            groupId = "bot-analytics-consumer-group"
    )
    public void consumeAnalyticsEvent(AnalyticsEventDto eventDto) {
        try {
            User userReference = userRepository.getReferenceById(eventDto.userId());

            AnalyticsEvent dbEvent = new AnalyticsEvent();
            dbEvent.setUser(userReference);
            dbEvent.setEventType(eventDto.type());
            dbEvent.setReferenceId(eventDto.refId());
            dbEvent.setMetadata(eventDto.meta());

            analyticsEventRepository.save(dbEvent);
            log.debug("Persisted analytics event [{}] for user {}", eventDto.type(), eventDto.userId());

        } catch (Exception e) {
            log.error("Failed to persist analytics event for user {}: {}", eventDto.userId(), e.getMessage());
        }
    }
}
