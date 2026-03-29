package md.botservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramAvatarServiceTest {

    private TelegramAvatarService telegramAvatarService;

    @Mock
    private RestClient mockRestClient;

    private RestClient.RequestHeadersUriSpec mockUriSpec;
    private RestClient.RequestHeadersSpec mockHeadersSpec;
    private RestClient.ResponseSpec mockResponseSpec;

    @BeforeEach
    @SuppressWarnings({"unchecked"})
    void setUp() {
        telegramAvatarService = new TelegramAvatarService();
        ReflectionTestUtils.setField(telegramAvatarService, "restClient", mockRestClient);

        mockUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        mockHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        mockResponseSpec = mock(RestClient.ResponseSpec.class);

        when(mockRestClient.get()).thenReturn(mockUriSpec);
        when(mockUriSpec.uri(anyString())).thenReturn(mockHeadersSpec);
        when(mockHeadersSpec.header(anyString(), any(String[].class))).thenReturn(mockHeadersSpec);
        when(mockHeadersSpec.retrieve()).thenReturn(mockResponseSpec);
    }

    @Test
    void fetchAvatarUrl_Success_ExtractsImageUrl() {
        String mockHtmlResponse = """
                <html>
                    <head>
                        <meta property="og:title" content="Telegram Channel"/>
                        <meta property="og:image" content="https://telegram.org/custom_avatar.jpg"/>
                        <meta property="og:description" content="Desc"/>
                    </head>
                    <body></body>
                </html>
                """;

        when(mockResponseSpec.body(String.class)).thenReturn(mockHtmlResponse);

        String result = telegramAvatarService.fetchAvatarUrl("testchannel");

        assertEquals("https://telegram.org/custom_avatar.jpg", result);
    }

    @Test
    void fetchAvatarUrl_IgnoresDefaultTelegramLogo() {
        String mockHtmlResponse = """
                <html>
                    <head>
                        <meta property="og:image" content="https://telegram.org/img/t_logo.png"/>
                    </head>
                </html>
                """;

        when(mockResponseSpec.body(String.class)).thenReturn(mockHtmlResponse);

        String result = telegramAvatarService.fetchAvatarUrl("testchannel");

        assertEquals("", result);
    }

    @Test
    void fetchAvatarUrl_NetworkException_ReturnsEmptyString() {
        when(mockResponseSpec.body(String.class)).thenThrow(new RuntimeException("Connection timeout"));

        String result = telegramAvatarService.fetchAvatarUrl("testchannel");

        assertEquals("", result);
    }

    @Test
    void fetchAvatarUrl_NoOgImageFound_ReturnsEmptyString() {
        String mockHtmlResponse = "<html><body>No meta tags here</body></html>";

        when(mockResponseSpec.body(String.class)).thenReturn(mockHtmlResponse);

        String result = telegramAvatarService.fetchAvatarUrl("testchannel");

        assertEquals("", result);
    }

}