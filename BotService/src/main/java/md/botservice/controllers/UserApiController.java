package md.botservice.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.dto.SourceRequest;
import md.botservice.dto.UserProfileResponse;
import md.botservice.dto.InterestRequest;
import md.botservice.service.SourceService;
import md.botservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class UserApiController {

    private final UserService userService;
    private final SourceService sourceService;

    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        log.info("📥 GET /api/users/{}/profile", userId);
        try {
            UserProfileResponse profile = userService.getUserProfile(userId);
            log.info("✅ Profile retrieved for user {}: {} interests, {} sources",
                    userId, profile.getInterests(), profile.getSources().size());
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("❌ Error getting profile for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/{userId}/interests")
    public ResponseEntity<Void> updateInterests(@PathVariable Long userId, @RequestBody InterestRequest request) {
        log.info("📥 POST /api/users/{}/interests: {}", userId, request.interest());
        try {
            userService.updateInterests(userId, request.interest());
            log.info("✅ Interests updated for user {}", userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ Error updating interests for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/{userId}/sources")
    public ResponseEntity<Void> addSource(@PathVariable Long userId, @RequestBody SourceRequest request) {
        log.info("📥 POST /api/users/{}/sources: {}", userId, request.source());
        try {
            String rawUrl = request.source();
            String normalizedUrl = normalizeTelegramUrl(rawUrl);

            log.info("🔗 Normalized URL: {} -> {}", rawUrl, normalizedUrl);

            sourceService.subscribeUser(userService.findById(userId), normalizedUrl);
            log.info("✅ Source added for user {}", userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ Error adding source for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/{userId}/sources")
    public ResponseEntity<Void> removeSource(@PathVariable Long userId, @RequestParam String url) {
        log.info("📥 DELETE /api/users/{}/sources?url={}", userId, url);
        try {
            sourceService.unsubscribeUser(userService.findById(userId), url);
            log.info("✅ Source removed for user {}", userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ Error removing source for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    // Health check endpoint for testing
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        log.info("📥 GET /api/users/health");
        return ResponseEntity.ok("OK - UserApiController is running");
    }

    private String normalizeTelegramUrl(String input) {
        if (input == null) return "";
        String clean = input.trim();

        if (clean.startsWith("http")) {
            return clean;
        }

        if (clean.startsWith("@")) {
            clean = clean.substring(1);
        }

        return "https://t.me/s/" + clean;
    }
}