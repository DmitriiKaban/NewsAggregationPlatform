package md.faf223.airecommendationsservice;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class AiRecommendationsServiceApplication {

    public static void main(String[] args) {
        loadEnvVariables();
        SpringApplication.run(AiRecommendationsServiceApplication.class, args);
    }

    private static void loadEnvVariables() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("../common")
                    .filename(".env")
                    .load();

            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });

            log.info("Successfully loaded .env variables!");
        } catch (Exception e) {
            log.error("Failed to load .env file. Check the path!");
            e.printStackTrace();
            System.exit(1);
        }
    }

}
