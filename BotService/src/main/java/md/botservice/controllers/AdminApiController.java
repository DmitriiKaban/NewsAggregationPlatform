package md.botservice.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.dto.SourceDto;
import md.botservice.exceptions.TelegramChannelNotFoundException;
import md.botservice.models.SourceType;
import md.botservice.models.TrustLevel;
import md.botservice.models.User;
import md.botservice.models.UserRole;
import md.botservice.service.SourceService;
import md.botservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final UserService userService;
    private final SourceService sourceService;

    private void verifyAdmin(Long adminId) {
        User admin = userService.findById(adminId);
        if (admin.getRole() != UserRole.ADMIN) {
            log.warn("Unauthorized access attempt by user: {}", adminId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized: User is not an admin");
        }
    }

    @GetMapping("/sources")
    public ResponseEntity<List<SourceDto>> getAllSources(@RequestParam Long adminId) {
        try {
            verifyAdmin(adminId);
            return ResponseEntity.ok(sourceService.getAllSources());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching global sources: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching sources", e);
        }
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Void> setUserRole(
            @PathVariable Long userId,
            @RequestParam UserRole role,
            @RequestParam Long adminId) {
        try {
            verifyAdmin(adminId);
            userService.updateUserRole(adminId, userId, role);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error setting role for user {}: {}", userId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error setting role", e);
        }
    }

    @PostMapping("/sources")
    public ResponseEntity<Void> addGlobalSource(
            @RequestParam String url,
            @RequestParam SourceType type,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) TrustLevel trustLevel,
            @RequestParam Long adminId) {
        try {
            verifyAdmin(adminId);
            sourceService.addGlobalSource(url, type, name, trustLevel);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (TelegramChannelNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Telegram link");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error adding global source {}: {}", url, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error adding source", e);
        }
    }

    @DeleteMapping("/sources/{sourceId}")
    public ResponseEntity<Void> deleteGlobalSource(
            @PathVariable Long sourceId,
            @RequestParam Long adminId) {
        try {
            verifyAdmin(adminId);
            sourceService.deleteGlobalSource(sourceId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting global source {}: {}", sourceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting source", e);
        }
    }

}