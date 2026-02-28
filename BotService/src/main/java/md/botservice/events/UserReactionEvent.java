package md.botservice.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import md.botservice.models.ReactionType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserReactionEvent {
    private Long userId;
    private String postId;
    private ReactionType reactionType;
}