package md.faf223.airecommendationsservice.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicClassifierService {

    private final EmbeddingService embeddingService;
    private final Map<String, float[]> topicCentroids = new HashMap<>();

    @PostConstruct
    public void initializeTopicVectors() {
        log.info("Initializing semantic topic vectors...");
        Map<String, String> topics = Map.ofEntries(
                Map.entry("Technology", "Technology, software development, gadgets, cybersecurity, programming, hardware, consumer electronics, smartphones, artificial intelligence, machine learning, ChatGPT, neural networks, LLMs, generative AI, robotics, OpenAI"),
                Map.entry("Science", "Science, space exploration, NASA, SpaceX, physics, astronomy, research discoveries, biology, chemistry"),
                Map.entry("Finance", "Finance, global economy, stock market, banking, inflation, interest rates, investment, wealth, wall street, cryptocurrency, bitcoin, ethereum, blockchain, web3, decentralized finance, crypto exchanges, tokens"),
                Map.entry("Business", "Business, entrepreneurship, startups, corporate earnings, mergers, acquisitions, venture capital, CEOs"),
                Map.entry("Politics", "Politics, government, elections, parliament, international relations, diplomacy, laws, treaties, geopolitics, campaigns"),
                Map.entry("Crime & Law", "Justice system, courts, police, investigations, lawsuits, legal rulings, crime, supreme court, prosecution"),
                Map.entry("Education", "Schools, universities, higher education, student life, academic research, teaching, learning, edtech, tuition"),
                Map.entry("Health", "Healthcare, medicine, diseases, wellness, hospitals, medical research, pandemics, nutrition, mental health, fitness"),
                Map.entry("Climate", "Climate change, global warming, renewable energy, ecology, conservation, sustainability, natural disasters, green tech"),
                Map.entry("Sports", "Sports, football, tennis, olympics, championships, athletes, tournaments, basketball, racing, boxing"),
                Map.entry("Entertainment", "Movies, music, celebrity news, hollywood, pop culture, television, streaming, art, theater, actors, video games, gaming consoles, esports, game development, PlayStation, Xbox, Nintendo, PC gaming"),
                Map.entry("Transport", "Cars, electric vehicles, Tesla, aviation, transportation, logistics, railways, public transit, auto industry"),
                Map.entry("Lifestyle", "Travel, tourism, food, restaurants, fashion, relationships, culture, hobbies, lifestyle, home design")
        );

        for (Map.Entry<String, String> entry : topics.entrySet()) {
            try {
                topicCentroids.put(entry.getKey(), embeddingService.encode("passage: " + entry.getValue()));
            } catch (Exception e) {
                log.error("Failed to initialize vector for topic {}", entry.getKey(), e);
            }
        }
    }

    public String predictTopic(float[] articleVector) {
        if (articleVector == null || articleVector.length == 0) {
            return "General News";
        }

        String bestTopic = "General News";
        double highest = -1.0;

        for (Map.Entry<String, float[]> entry : topicCentroids.entrySet()) {
            double sim = calculateCosineSimilarity(articleVector, entry.getValue());
            if (sim > highest) {
                highest = sim;
                bestTopic = entry.getKey();
            }
        }
        return highest > 0.55 ? bestTopic : "General News";
    }

    private double calculateCosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0.0;
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return (normA == 0 || normB == 0) ? 0.0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public String toVectorString(float[] vector) {
        return Arrays.toString(vector);
    }

}