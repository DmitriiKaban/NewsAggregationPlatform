package md.botservice;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BotServiceApplication {

	public static void main(String[] args) {
		loadEnvVariables();
		SpringApplication.run(BotServiceApplication.class, args);
	}

	private static void loadEnvVariables() {
		try {
			Dotenv dotenv = Dotenv.configure()
                    .directory("common")
					.filename(".env")
					.load();

			dotenv.entries().forEach(entry -> {
				System.setProperty(entry.getKey(), entry.getValue());
			});

			System.out.println("✅ Successfully loaded .env variables!");

		} catch (Exception e) {
			System.err.println("❌ FAILED to load .env file. Check the path!");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
