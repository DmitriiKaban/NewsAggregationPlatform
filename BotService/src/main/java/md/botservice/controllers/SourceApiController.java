package md.botservice.controllers;

import lombok.RequiredArgsConstructor;
import md.botservice.service.TelegramAvatarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sources")
@RequiredArgsConstructor
public class SourceApiController {

    private final TelegramAvatarService avatarService;

    @GetMapping("/avatar")
    public ResponseEntity<Map<String, String>> getSourceAvatar(@RequestParam String handle) {
        String cleanHandle = handle.replace("https://t.me/", "").replace("@", "").trim();
        if (cleanHandle.contains("/")) {
            cleanHandle = cleanHandle.substring(cleanHandle.lastIndexOf("/") + 1);
        }

        String imageUrl = avatarService.fetchAvatarUrl(cleanHandle);

        if (imageUrl == null || imageUrl.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("url", imageUrl));
    }

}