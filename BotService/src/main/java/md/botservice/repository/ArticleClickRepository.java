package md.botservice.repository;

import md.botservice.models.ArticleClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleClickRepository extends JpaRepository<ArticleClick, Long> {
}
