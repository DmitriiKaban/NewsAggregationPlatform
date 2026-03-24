package md.botservice.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.dto.AnalyticsEventDto;
import md.botservice.models.AnalyticsEvent;
import md.botservice.models.ArticleClick;
import md.botservice.repository.AnalyticsEventRepository;
import md.botservice.repository.ArticleClickRepository;
import md.botservice.repository.UserRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsConsumer {

    private final AnalyticsEventRepository analyticsEventRepository;
    private final UserRepository userRepository;
    private final ArticleClickRepository articleClickRepository;

    @KafkaListener(
            topics = "bot.analytics.events",
            groupId = "bot-analytics-consumer-group",
            containerFactory = "analyticsListenerFactory"
    )
    public void consumeAnalyticsEvent(AnalyticsEventDto eventDto) {
        try {
            AnalyticsEvent dbEvent = new AnalyticsEvent();
            dbEvent.setUser(userRepository.getReferenceById(eventDto.userId()));
            dbEvent.setEventType(eventDto.type());
            dbEvent.setReferenceId(eventDto.refId());
            dbEvent.setMetadata(eventDto.meta());
            analyticsEventRepository.save(dbEvent);

            boolean isClickEvent = "ARTICLE_OPENED".equals(eventDto.type()) ||
                    (eventDto.type() != null && eventDto.type().startsWith("REACTION_"));

            if (isClickEvent && eventDto.refId() != null) {
                try {
                    ArticleClick click = new ArticleClick();
                    click.setUserId(eventDto.userId());
                    click.setArticleId(Long.parseLong(eventDto.refId()));
                    articleClickRepository.save(click);
                    log.debug("Persisted dedicated ArticleClick proxy for user {} and article {}", eventDto.userId(), eventDto.refId());
                } catch (NumberFormatException e) {
                    log.warn("Could not parse refId [{}] into article_id for clicks table", eventDto.refId());
                }
            }
            log.debug("Persisted analytics event [{}] for user {}", eventDto.type(), eventDto.userId());

        } catch (Exception e) {
            log.error("Failed to persist analytics event for user {}: {}", eventDto.userId(), e.getMessage());
        }
    }
}
