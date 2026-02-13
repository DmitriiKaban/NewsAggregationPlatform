package md.botservice.controllers;

import lombok.RequiredArgsConstructor;
import md.botservice.models.User;
import md.botservice.requests.InterestRequest;
import md.botservice.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "https://dmitriikaban.github.io")
public class UserApiController {

    private final UserService userService;

    @GetMapping("/{userId}/interests")
    public List<String> getUserInterests(@PathVariable Long userId) {
        User user = userService.findById(userId);
        return user.getInterestsRaw();
    }

    @PostMapping("/{userId}/interests")
    public void addInterest(@PathVariable Long userId, @RequestBody InterestRequest request) {
        userService.addInterest(userId, request.getInterest());
    }

    @GetMapping("/{userId}/sources")
    public List<String> getUserSources(@PathVariable Long userId) {
        User user = userService.findById(userId);
        return user.getSources(); // Assuming you have this field
    }
}
