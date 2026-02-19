package md.botservice.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    @Column(name = "show_only_subscribed_sources", nullable = false)
    private boolean showOnlySubscribedSources = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_subscriptions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "source_id")
    )
    private Set<Source> subscriptions = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_read_all_sources",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "source_id")
    )
    private Set<Source> readAllPostsSources = new HashSet<>();
}