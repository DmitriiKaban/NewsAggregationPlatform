package md.botservice.service;

import md.botservice.models.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private MessageService messageService;

    @Test
    @SuppressWarnings("DataFlowIssue")
    void getMessage_WithoutArgs_CallsMessageSource() {
        when(messageSource.getMessage(eq("test.key"), isNull(), any(String.class), any(Locale.class)))
                .thenReturn("Test Message");

        String result = messageService.get("test.key", Language.EN);

        assertEquals("Test Message", result);
    }

    @Test
    void getMessage_WithArgs_CallsMessageSource() {
        Object[] args = {"arg1", "arg2"};

        when(messageSource.getMessage(eq("test.key.args"), eq(args), any(String.class), any(Locale.class)))
                .thenReturn("Test Message arg1");

        String result = messageService.get("test.key.args", Language.EN, args);

        assertEquals("Test Message arg1", result);
    }

}