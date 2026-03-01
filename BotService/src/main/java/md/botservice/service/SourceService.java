package md.botservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.dto.TopSourceProjection;
import md.botservice.exceptions.SourceNotFoundException;
import md.botservice.exceptions.TelegramChannelNotFoundException;
import md.botservice.models.Source;
import md.botservice.models.SourceType;
import md.botservice.models.User;
import md.botservice.repository.SourceRepository;
import md.botservice.utils.FormatUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SourceService {

    private final SourceRepository sourceRepository;
    private final UserService userService;
    private final SourceUpdatePublisher sourceUpdatePublisher;
    private final RestTemplate restTemplate;

    @Transactional
    public void subscribeUser(User user, String url) {
        String cleanUrl = FormatUtils.normalizeTelegramUrl(url);

        if (!verifyTelegramChannel(cleanUrl)) {
            throw new TelegramChannelNotFoundException("Telegram channel not found or not accessible: " + url);
        }

        Source source = findOrSaveSource(cleanUrl);

        user.getSubscriptions().add(source);
        user = userService.updateUser(user);

        sourceUpdatePublisher.publishSourceUpdate(user);

        log.info("User {} subscribed to source: {}", user.getId(), cleanUrl);
    }

    @Transactional
    public void unsubscribeUser(User user, String url) {
        String fullUrl = FormatUtils.normalizeTelegramUrl(url);

        user.getSubscriptions().removeIf(s -> s.getUrl().equals(fullUrl));
        user = userService.updateUser(user);

        // Publish to Kafka for AI service
        sourceUpdatePublisher.publishSourceUpdate(user);

        log.info("User {} unsubscribed from source: {}", user.getId(), fullUrl);
    }

    @Transactional
    public void unsubscribeUser(User user, Long sourceId) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new SourceNotFoundException("Source not found"));

        user.getSubscriptions().remove(source);
        user = userService.updateUser(user);

        // Publish to Kafka for AI service
        sourceUpdatePublisher.publishSourceUpdate(user);

        log.info("User {} unsubscribed from source ID: {}", user.getId(), sourceId);
    }

    private boolean verifyTelegramChannel(String url) {
        try {
            log.info("Verifying Telegram channel: {}", url);

            // Make a HEAD request to check if channel exists
            restTemplate.headForHeaders(url);

            log.info("Channel verified: {}", url);
            return true;

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Channel not found: {}", url);
            return false;
        } catch (Exception e) {
            log.warn("Could not verify channel (assuming exists): {} - {}", url, e.getMessage());
            return true;
        }
    }

    private Source findOrSaveSource(String cleanUrl) {
        return sourceRepository.findByUrl(cleanUrl)
                .orElseGet(() -> {
                    Source newSource = new Source();
                    newSource.setUrl(cleanUrl);
                    newSource.setType(SourceType.TELEGRAM);
                    newSource.setName(extractName(cleanUrl));
                    return sourceRepository.save(newSource);
                });
    }

    private String extractName(String url) {
        if (url.contains("/")) {
            return url.substring(url.lastIndexOf("/") + 1);
        }
        return url;
    }

    @Transactional
    public void setShowOnlySubscribedSources(User user, boolean enabled) {
        user.setShowOnlySubscribedSources(enabled);
        user = userService.updateUserFiltering(user.getId(), enabled);

        sourceUpdatePublisher.publishSourceUpdate(user);

        log.info("User {} set showOnlySubscribedSources to: {}", user.getId(), enabled);
    }

    public List<TopSourceProjection> getTopSources() {
        return sourceRepository.getTopSources();
    }
}