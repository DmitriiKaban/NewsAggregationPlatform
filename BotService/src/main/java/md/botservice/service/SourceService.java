package md.botservice.service;

import lombok.RequiredArgsConstructor;
import md.botservice.model.Source;
import md.botservice.model.SourceType;
import md.botservice.model.User;
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
        user.getSubscriptions().removeIf(s -> s.getUrl().equals(url));
        userService.updateUser(user);
    }

    private String extractName(String url) {
        if (url.contains("/")) {
            return url.substring(url.lastIndexOf("/") + 1);
        }
        return url;
    }
}
