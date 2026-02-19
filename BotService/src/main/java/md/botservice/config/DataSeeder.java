package md.botservice.config;

import lombok.RequiredArgsConstructor;
import md.botservice.models.Source;
import md.botservice.models.SourceType;
import md.botservice.models.TrustLevel;
import md.botservice.repository.SourceRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final SourceRepository sourceRepository;

    @Override
    public void run(String @NonNull ... args) {
        if (sourceRepository.count() == 0) {
            System.out.println("🌱 Database is empty. Seeding sources...");
            seedSources();
            System.out.println("✅ Seeding complete!");
        }
    }
    private void seedSources() {
        List<Source> defaults = List.of(
                // --- MOLDOVA ---
                create("Moldpres", "https://www.moldpres.md/config/rss.php?lang=rom", SourceType.RSS, TrustLevel.OFFICIAL),
                create("Ziarul de Gardă", "https://t.me/zdgmd", SourceType.TELEGRAM, TrustLevel.VERIFIED_MEDIA),
                create("Agora.md", "https://t.me/agoramd", SourceType.TELEGRAM, TrustLevel.VERIFIED_MEDIA),
                create("Point News", "https://t.me/PointNews", SourceType.TELEGRAM, TrustLevel.VERIFIED_MEDIA),
                create("Chisinau vteme", "https://t.me/vtememd", SourceType.TELEGRAM, TrustLevel.USER_GENERATED_CONTENT),

                // --- WORLD NEWS (Official RSS) ---
                create("BBC World", "https://feeds.bbci.co.uk/news/world/rss.xml", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),
                create("The Guardian", "https://www.theguardian.com/world/rss", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),
                create("Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),

                // --- FINANCE & BUSINESS ---
                create("CNBC International", "https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=10000664", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),
                create("Fortune", "https://fortune.com/feed/", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),

                // --- SCIENCE & NATURE ---
                create("Nature Journal", "http://www.nature.com/nature/current_issue/rss/", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),
                create("Science Daily", "https://www.sciencedaily.com/rss/top/science.xml", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),
                create("TechCrunch", "https://techcrunch.com/feed/", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),
                create("The Verge", "https://www.theverge.com/rss/index.xml", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),

                // --- TED TALKS ---
                create("TED Talks", "https://www.ted.com/talks/rss", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),

                // --- SPORT ---
                create("Motorsport.com (F1)", "https://www.motorsport.com/rss/f1/news/", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),
                create("ESPN", "https://www.espn.com/espn/rss/news", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),

                // --- FASHION & LIFESTYLE ---
                create("Vogue", "https://www.vogue.com/feed/rss", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),
                create("Elle Romania", "https://www.elle.ro/feed/", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),
                create("Business of Fashion", "https://www.businessoffashion.com/feeds/all", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),

                // --- HEALTH ---
                create("Medical News Today", "https://www.medicalnewstoday.com/feed", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),

                // --- FUN ---
                create("Programmer Jokes", "https://t.me/programmerjokes", SourceType.TELEGRAM, TrustLevel.USER_GENERATED_CONTENT)
        );

        sourceRepository.saveAll(defaults);
    }

    private Source create(String name, String url, SourceType type, TrustLevel trust) {
        Source s = new Source();
        s.setName(name);
        s.setUrl(url);
        s.setType(type);
        s.setTrustLevel(trust);
        s.setActive(true);
        return s;
    }
}