package md.botservice.service;

import lombok.RequiredArgsConstructor;
import md.botservice.models.Language;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    public String get(String key, Language language) {
        Locale locale = new Locale(language.getCode());

        return messageSource.getMessage(key, null, "Missing translation: " + key, locale);
    }

    public String get(String key, Language language, Object... args) {
        Locale locale = new Locale(language.getCode());

        return messageSource.getMessage(key, args, "Missing translation: " + key, locale);
    }
}