package md.botservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.dto.SourceDto;
import md.botservice.events.UserInterestEvent;
import md.botservice.dto.UserProfileResponse;
import md.botservice.exceptions.UserNotFoundException;
import md.botservice.models.Source;
import md.botservice.models.User;
import md.botservice.repository.UserRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SourceUpdatePublisher sourceUpdatePublisher;
    private static final String TOPIC_USER_INTERESTS = "user.interests.updated";

    public User findOrRegister(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        return repository.findById(telegramUser.getId())
                .orElseGet(() -> registerUser(telegramUser));
    }

    private User registerUser(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        User user = new User();
        user.setId(telegramUser.getId());
        user.setUsername(telegramUser.getUserName());
        user.setFirstName(telegramUser.getFirstName());
        user.setRegisteredAt(LocalDateTime.now());
        return repository.save(user);
    }

    public UserProfileResponse getUserProfile(Long userId) {
        User user = findById(userId);

        List<SourceDto> sourceDtos = user.getSubscriptions().stream()
                .map(s -> SourceDto.of(
                        s.getId(),
                        s.getName(),
                        s.getUrl(),
                        user.getReadAllPostsSources().contains(s)
                ))
                .toList();

        return new UserProfileResponse(
                user.getFirstName(),
                user.getLastName(),
                user.getInterestsRaw(),
                sourceDtos,
                user.isShowOnlySubscribedSources()
        );
    }

    public void updateInterests(Long userId, String rawInterests) {
        User user = findById(userId);
        user.setInterestsRaw(rawInterests);
        repository.save(user);

        try {
            UserInterestEvent event = new UserInterestEvent(userId, rawInterests);
            kafkaTemplate.send(TOPIC_USER_INTERESTS, event);

            log.info("✅ Sent interest update for user {} to Kafka: {}", userId, rawInterests);
        } catch (Exception e) {
            log.error("❌ Failed to send interest update to Kafka", e);
        }
    }

    public User updateUser(User user) {
        return repository.save(user);
    }

    public User findById(Long userId) {
        return repository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Couldn't find user with id: " + userId));
    }

    public User updateUserFiltering(Long userId, boolean enabled) {
        User user = findById(userId);
        user.setShowOnlySubscribedSources(enabled);
        user = repository.save(user);

        // Publish to Kafka for AI service
        sourceUpdatePublisher.publishSourceUpdate(user);

        log.info("✅ User {} set showOnlySubscribedSources to: {}", userId, enabled);
        return user;
    }

    public User addReadAllNewsSource(User user, Source source) {
        user.getReadAllPostsSources().add(source);
        return repository.save(user);
    }

    public User removeReadAllNewsSource(User user, Source source) {
        user.getReadAllPostsSources().remove(source);
        return repository.save(user);
    }

    public void updateReadAllNewsSource(Long userId, Long sourceId, boolean readAll) {
        User user = findById(userId);

        // Find source in user's subscriptions
        Source source = user.getSubscriptions().stream()
                .filter(s -> s.getId().equals(sourceId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Source not found in user subscriptions"));

        if (readAll) {
            user.getReadAllPostsSources().add(source);
        } else {
            user.getReadAllPostsSources().remove(source);
        }

        user = repository.save(user);

        // Publish to Kafka for AI service
        sourceUpdatePublisher.publishSourceUpdate(user);

        log.info("✅ User {} {} read-all for source {}", userId, readAll ? "enabled" : "disabled", sourceId);
    }
}