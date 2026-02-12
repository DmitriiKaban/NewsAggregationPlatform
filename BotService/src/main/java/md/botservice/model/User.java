package md.botservice.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    private Long telegramId;
    private String username;
    private String firstName;

    private String interests;
    private boolean active = true;
}
