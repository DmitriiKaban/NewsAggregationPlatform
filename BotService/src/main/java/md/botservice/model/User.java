package md.botservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
@Data
@NoArgsConstructor
public class User {
    @Id
    private Long id;

    private String username;
    private String firstName;
    private String lastName;

    @Column(columnDefinition = "TEXT")
    private String interestsRaw;
    private LocalDateTime registeredAt;
}
