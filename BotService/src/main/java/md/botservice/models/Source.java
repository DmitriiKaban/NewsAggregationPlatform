package md.botservice.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sources")
@Getter
@Setter
public class Source {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String url;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType type;

    @Enumerated(EnumType.STRING)
    private TrustLevel trustLevel = TrustLevel.USER_GENERATED_CONTENT;

    private boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Source source)) return false;
        return id != null && id.equals(source.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}