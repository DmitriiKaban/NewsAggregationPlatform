package md.botservice.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.models.Article;
import md.botservice.producers.EventTrackingService;
import md.botservice.repository.ArticleRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

@Slf4j
@Controller
@RequestMapping("/api/r")
@RequiredArgsConstructor
public class RedirectController {

    private final EventTrackingService eventTrackingService;
    private final ArticleRepository articleRepository;

    @GetMapping("/{userId}/{articleId}")
    public RedirectView trackAndRedirect(@PathVariable Long userId, @PathVariable Long articleId) {
        Article article = articleRepository.findById(articleId).orElse(null);

        if (article == null) {
            return new RedirectView("https://DmitriiKaban.github.io/NewsAggregationPlatform/");
        }

        eventTrackingService.trackArticleOpened(
                userId,
                String.valueOf(articleId),
                "Unknown",
                "Unknown"
        );

        log.info("Tracked click for User {} on Article {}", userId, articleId);

        return new RedirectView(article.getUrl());
    }
}
