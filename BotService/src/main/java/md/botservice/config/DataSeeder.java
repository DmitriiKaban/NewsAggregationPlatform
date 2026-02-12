package md.botservice.config;

import lombok.RequiredArgsConstructor;
import md.botservice.model.Source;
import md.botservice.model.SourceType;
import md.botservice.model.TrustLevel;
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
                // --- 🇲🇩 MOLDOVA OFFICIAL & NEWS ---
                create("Moldpres", "https://www.moldpres.md/config/rss.php?lang=rom", SourceType.RSS, TrustLevel.OFFICIAL),
                create("Ziarul de Gardă", "https://t.me/zdgmd", SourceType.TELEGRAM, TrustLevel.VERIFIED_MEDIA),
                create("Point News", "https://t.me/PointNews", SourceType.TELEGRAM, TrustLevel.VERIFIED_MEDIA),
                create("Agora.md", "https://t.me/agoramd", SourceType.TELEGRAM, TrustLevel.VERIFIED_MEDIA),

                // --- 🏆 SPORT (Diverse) ---
                create("ESPN Top News", "https://www.espn.com/espn/rss/news", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),
                create("Motorsport.com", "https://www.motorsport.com/rss/f1/news/", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),
                create("Tennis.com", "http://www.tennis.com/rss", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),

                // --- 👗 FASHION & LIFESTYLE ---
                create("Vogue", "https://www.vogue.com/feed/rss", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),
                create("Elle Romania", "https://www.elle.ro/feed/", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),
                create("Hypebeast", "https://hypebeast.com/feeds/rss", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),
                create("Business of Fashion", "https://www.businessoffashion.com/feeds/all", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),

                // --- 💻 TECH & SCIENCE ---
                create("TechCrunch", "https://techcrunch.com/feed/", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),
                create("The Verge", "https://www.theverge.com/rss/index.xml", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),
                create("ScienceDaily", "https://www.sciencedaily.com/rss/top/science.xml", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),

                // --- 🏥 HEALTH ---
                create("Medical News Today", "https://www.medicalnewstoday.com/feed", SourceType.RSS, TrustLevel.VERIFIED_MEDIA),

                // --- 🌍 WORLD NEWS ---
                create("BBC World", "https://feeds.bbci.co.uk/news/world/rss.xml", SourceType.RSS, TrustLevel.VERIFIED_MEDIA)
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