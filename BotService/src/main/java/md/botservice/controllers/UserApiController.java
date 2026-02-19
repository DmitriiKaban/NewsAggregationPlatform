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
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.PUT})
public class UserApiController {

    private final UserService userService;
    private final SourceService sourceService;

    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        try {
            UserProfileResponse profile = userService.getUserProfile(userId);
            log.info("Profile retrieved for user {}", userId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("Error getting profile for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/{userId}/interests")
    public ResponseEntity<Void> updateInterests(@PathVariable Long userId, @RequestBody InterestRequest request) {
        try {
            userService.updateInterests(userId, request.interest());
            log.info("Interests updated for user {}", userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating interests for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/{userId}/sources")
    public ResponseEntity<Void> addSource(@PathVariable Long userId, @RequestBody SourceRequest request) {
        try {
            sourceService.subscribeUser(userService.findById(userId), request.source());
            log.info("Source added for user {}", userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error adding source for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/{userId}/sources")
    public ResponseEntity<Void> removeSource(@PathVariable Long userId, @RequestParam String url) {
        try {
            sourceService.unsubscribeUser(userService.findById(userId), url);
            log.info("Source removed for user {}", userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error removing source for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    @PutMapping("/{userId}/settings/strict-filtering")
    public ResponseEntity<Void> toggleStrictFiltering(
            @PathVariable Long userId,
            @RequestParam boolean enabled
    ) {
        try {
            userService.updateUserFiltering(userId, enabled);
            log.info("User {} set strict filtering to: {}", userId, enabled);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating strict filtering for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    @PutMapping("/{userId}/sources/{sourceId}/read-all")
    public ResponseEntity<Void> toggleSourceReadAll(
            @PathVariable Long userId,
            @PathVariable Long sourceId,
            @RequestParam boolean readAll
    ) {
        try {
            userService.updateReadAllNewsSource(userId, sourceId, readAll);
            log.info("User {} source {} read-all set to: {}", userId, sourceId, readAll);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating read-all for user {}, source {}: {}", userId, sourceId, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK - UserApiController is running");
    }
}