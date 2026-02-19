package md.botservice.events;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserInterestEvent {
    private Long userId;
    private String interests;
}
