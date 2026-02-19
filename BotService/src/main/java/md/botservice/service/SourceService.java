package md.botservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.exceptions.SourceNotFound;
import md.botservice.models.Source;
import md.botservice.models.SourceType;
import md.botservice.models.User;
import md.botservice.repository.SourceRepository;
import md.botservice.utils.FormatUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SourceService {

    private final SourceRepository sourceRepository;
    private final UserService userService;
    private final SourceUpdatePublisher sourceUpdatePublisher;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public void subscribeUser(User user, String url) {
        String cleanUrl = FormatUtils.normalizeTelegramUrl(url);

        // Verify Telegram channel exists
        if (!verifyTelegramChannel(cleanUrl)) {
            throw new RuntimeException("❌ Telegram channel not found or not accessible: " + url);
        }

        Source source = findOrSaveSource(cleanUrl);

        user.getSubscriptions().add(source);
        user = userService.updateUser(user);

        // Publish to Kafka for AI service
        sourceUpdatePublisher.publishSourceUpdate(user);

        log.info("✅ User {} subscribed to source: {}", user.getId(), cleanUrl);
    }

    @Transactional
    public void unsubscribeUser(User user, String url) {
        String fullUrl = FormatUtils.normalizeTelegramUrl(url);

        user.getSubscriptions().removeIf(s -> s.getUrl().equals(fullUrl));
        user = userService.updateUser(user);

        // Publish to Kafka for AI service
        sourceUpdatePublisher.publishSourceUpdate(user);

        log.info("✅ User {} unsubscribed from source: {}", user.getId(), fullUrl);
    }

    @Transactional
    public void unsubscribeUser(User user, Long sourceId) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new RuntimeException("Source not found"));

        user.getSubscriptions().remove(source);
        user = userService.updateUser(user);

        // Publish to Kafka for AI service
        sourceUpdatePublisher.publishSourceUpdate(user);

        log.info("✅ User {} unsubscribed from source ID: {}", user.getId(), sourceId);
    }

    /**
     * Verify that a Telegram channel exists and is accessible
     * Tries to fetch the channel's preview page
     */
    private boolean verifyTelegramChannel(String url) {
        try {
            log.info("🔍 Verifying Telegram channel: {}", url);

            // Make a HEAD request to check if channel exists
            restTemplate.headForHeaders(url);

            log.info("✅ Channel verified: {}", url);
            return true;

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("❌ Channel not found: {}", url);
            return false;
        } catch (Exception e) {
            log.warn("⚠️  Could not verify channel (assuming exists): {} - {}", url, e.getMessage());
            // If we can't verify (network issues, etc), assume it exists
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

        log.info("✅ User {} set showOnlySubscribedSources to: {}", user.getId(), enabled);
    }

    @Transactional
    public void addReadAllSource(User user, String url) {
        Source source = sourceRepository.findByUrl(url)
                .orElseGet(() -> {
                    Source newSource = new Source();
                    newSource.setUrl(url);
                    return sourceRepository.save(newSource);
                });

        user = userService.addReadAllNewsSource(user, source);

        sourceUpdatePublisher.publishSourceUpdate(user);

        log.info("✅ User {} added read-all source: {}", user.getId(), url);
    }

    @Transactional
    public void removeReadAllSource(User user, String url) {
        Source source = sourceRepository.findByUrl(url)
                .orElseThrow(() -> new RuntimeException("Source not found: " + url));

        userService.removeReadAllNewsSource(user, source);

        sourceUpdatePublisher.publishSourceUpdate(user);

        log.info("✅ User {} removed read-all source: {}", user.getId(), url);
    }

    public Source findByUrl(String url) {
        String fullUrl = FormatUtils.normalizeTelegramUrl(url);
        return sourceRepository.findByUrl(fullUrl)
                .orElseThrow(() -> new SourceNotFound("Couldn't find source with url: " + url));
    }

    public void updateUserSourceFiltering(Long userId, String url, boolean readAll) {
        Source source = findByUrl(url);
        userService.updateReadAllNewsSource(userId, source.getId(), readAll);
    }
}