package md.botservice.service;

import lombok.RequiredArgsConstructor;
import md.botservice.dto.SourceDto;
import md.botservice.dto.UserProfileResponse;
import md.botservice.exceptions.UserNotFoundException;
import md.botservice.models.User;
import md.botservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;

    public User findOrRegister(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        return repository.findById(telegramUser.getId())
                .orElseGet(() -> registerUser(telegramUser));
    }

    private User registerUser(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        User user = new User();
        user.setId(telegramUser.getId());
        user.setUsername(telegramUser.getUserName());
        user.setFirstName(telegramUser.getFirstName());
        user.setRegisteredAt(LocalDateTime.now());
        return repository.save(user);
    }

    public UserProfileResponse getUserProfile(Long userId) {
        User user = findById(userId);

        List<SourceDto> sourceDtos = user.getSubscriptions().stream()
                .map(s -> new SourceDto(s.getName(), s.getUrl()))
                .toList();

        return new UserProfileResponse(user.getFirstName(), user.getLastName(), user.getInterestsRaw(), sourceDtos);
    }

    public void updateInterests(Long userId, String rawInterests) {
        User user = findById(userId);
        user.setInterestsRaw(rawInterests);
        repository.save(user);
    }

    public void updateUser(User user) {
        repository.save(user);
    }

    public User findById(Long userId) {
        return repository.findById(userId).orElseThrow(() -> new UserNotFoundException("Couldn't find user with id: " + userId));
    }
}
