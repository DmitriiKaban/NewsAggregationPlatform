package md.botservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class UserProfileResponse {
    private String interests;
    private List<SourceDto> sources;

    @Data
    @AllArgsConstructor
    public static class SourceDto {
        private String name;
        private String url;
    }
}
