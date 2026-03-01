package md.botservice.controllers;

import md.botservice.dto.SourceDto;
import md.botservice.dto.UserProfileResponse;
import md.botservice.exceptions.GlobalExceptionHandler;
import md.botservice.exceptions.UserNotFoundException;
import md.botservice.service.SourceService;
import md.botservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(UserApiController.class)
@Import(GlobalExceptionHandler.class)
class UserApiControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private SourceService sourceService;

    @Test
    @DisplayName("Test for the getUserProfile method, should return user profile")
    void getUserProfile_shouldReturnUser() throws Exception {
        // Arrange
        long userId = 1L;
        String firstName = "Dmitrii";
        String lastName = "Cravcenco";
        String interests = "Interests";
        List<SourceDto> sourceDtoList = List.of(
                SourceDto.of(1L, "source1", "url1", false),
                SourceDto.of(2L, "source2", "url2", true)
                );
        boolean strictSourceFiltering = false;
        String language = "en";
        UserProfileResponse userProfileResponse = new UserProfileResponse(
                firstName, lastName, interests, sourceDtoList, strictSourceFiltering, language
        );
        when(userService.getUserProfile(userId)).thenReturn(userProfileResponse);

        // Act & Assert
        mockMvc.perform(get("/api/users/{userId}/profile", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value(firstName))
                .andExpect(jsonPath("$.lastName").value(lastName))
                .andExpect(jsonPath("$.interests").value(interests))
                .andExpect(jsonPath("$.sources.size()").value(2))
                .andExpect(jsonPath("$.sources[0].id").value(1))
                .andExpect(jsonPath("$.sources[0].name").value("source1"))
                .andExpect(jsonPath("$.sources[0].url").value("@url1"))
                .andExpect(jsonPath("$.sources[0].readAllNewsSource").value(false))
                .andExpect(jsonPath("$.sources[1].id").value(2))
                .andExpect(jsonPath("$.sources[1].name").value("source2"))
                .andExpect(jsonPath("$.sources[1].url").value("@url2"))
                .andExpect(jsonPath("$.sources[1].readAllNewsSource").value(true))
                .andExpect(jsonPath("$.strictSourceFiltering").value(false))
                .andExpect(jsonPath("$.language").value("en"));
    }

    @Test
    @DisplayName("Test for the getUserProfile method, should throw user not found exception")
    void getUserProfile_shouldThrowErrorUserNotFound() throws Exception {
        // Arrange
        long userId = 1L;
        when(userService.getUserProfile(userId)).thenThrow(new UserNotFoundException("Couldn't find user with id: " + userId));

        // Act & Assert
        mockMvc.perform(get("/api/users/{userId}/profile", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Couldn't find user with id: 1"));
    }

    @Test
    void updateInterests() {
    }

    @Test
    void addSource() {
    }

    @Test
    void removeSource() {
    }

    @Test
    void toggleStrictFiltering() {
    }

    @Test
    void toggleSourceReadAll() {
    }

    @Test
    void health() {
    }
}