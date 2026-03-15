package md.botservice.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id")
    private Long articleId;

    @Column(name = "source_id")
    private Long sourceId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_id", insertable = false, updatable = false)
    private Source source;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reported_user_id")
    private User reportedUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private ReportReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @CreationTimestamp
    @Column(name = "reported_at", updatable = false)
    private LocalDateTime reportedAt;

}