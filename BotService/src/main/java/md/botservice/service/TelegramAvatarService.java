package md.botservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TelegramAvatarService {

    private static final Pattern OG_IMAGE_PATTERN = Pattern.compile("<meta\\s+property=\"og:image\"\\s+content=\"([^\"]+)\"");

    private final RestClient restClient = RestClient.create();

    @Cacheable(value = "avatars", key = "#cleanHandle")
    public String fetchAvatarUrl(String cleanHandle) {
        try {
            String html = restClient.get()
                    .uri("https://t.me/" + cleanHandle)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .retrieve()
                    .body(String.class);

            if (html != null) {
                Matcher matcher = OG_IMAGE_PATTERN.matcher(html);
                if (matcher.find()) {
                    String imageUrl = matcher.group(1);
                    if (!imageUrl.contains("t_logo") && !imageUrl.contains("apple-touch-icon")) {
                        return imageUrl;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch avatar for {}: {}", cleanHandle, e.getMessage());
        }

        return "";
    }

}