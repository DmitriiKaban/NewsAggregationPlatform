package md.botservice.service;

import lombok.RequiredArgsConstructor;
import md.botservice.model.User;
import md.botservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

    public void updateUser(User user) {
        repository.save(user);
    }
}
