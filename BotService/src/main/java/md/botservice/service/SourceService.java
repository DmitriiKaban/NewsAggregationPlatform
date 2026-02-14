package md.botservice.service;

import lombok.RequiredArgsConstructor;
import md.botservice.models.Source;
import md.botservice.models.SourceType;
import md.botservice.models.User;
import md.botservice.repository.SourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SourceService {

    private final SourceRepository sourceRepository;
    private final UserService userService;

    @Transactional
    public void subscribeUser(User user, String url) {
        String cleanUrl = url.trim();

        // todo: use regex, improve versatility
        if (!url.startsWith("https://t.me/s/")) {
            cleanUrl = "https://t.me/s/" + cleanUrl;
        }
        Source source = findOrSaveSource(cleanUrl);

        user.getSubscriptions().add(source);
        userService.updateUser(user);
    }

    private Source findOrSaveSource(String cleanUrl) {
        return sourceRepository.findByUrl(cleanUrl)
                .orElseGet(() -> {
                    Source newSource = new Source();
                    newSource.setUrl(cleanUrl);
                    newSource.setType(SourceType.TELEGRAM); // user can add only telegram sources
                    newSource.setName(extractName(cleanUrl));
                    return sourceRepository.save(newSource);
                });
    }

    @Transactional
    public void unsubscribeUser(User user, String url) {

        String fullUrl;
        // todo: use regex, improve versatility
        if (!url.startsWith("https://t.me/s/")) {
            fullUrl = "https://t.me/s/" + url;
        } else {
            fullUrl = url;
        }

        user.getSubscriptions().removeIf(s -> s.getUrl().equals(fullUrl));
        userService.updateUser(user);
    }

    @Transactional
    public void unsubscribeUser(User user, Long sourceId) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new RuntimeException("Source not found"));
        user.getSubscriptions().remove(source);
        userService.updateUser(user);
    }

    private String extractName(String url) {
        if (url.contains("/")) {
            return url.substring(url.lastIndexOf("/") + 1);
        }
        return url;
    }
}
