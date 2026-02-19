package md.botservice.dto;

import java.util.List;

public record UserProfileResponse(
        String firstName,
        String lastName,
        String interests,
        List<SourceDto> sources,
        boolean strictSourceFiltering
) {
}
