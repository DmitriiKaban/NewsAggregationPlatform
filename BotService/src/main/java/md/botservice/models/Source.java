package md.botservice.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sources")
@Data
public class Source {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String url;

    private String name;

    @Enumerated(EnumType.STRING)
    private SourceType type;

    @Enumerated(EnumType.STRING)
    private TrustLevel trustLevel = TrustLevel.USER_GENERATED_CONTENT;

    private boolean isActive = true;
}
