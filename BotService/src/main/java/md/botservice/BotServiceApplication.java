package md.botservice;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class BotServiceApplication {

	public static void main(String[] args) {
		loadEnvVariables();
		SpringApplication.run(BotServiceApplication.class, args);
	}

	private static void loadEnvVariables() {
		String[] pathsToTry = {"../common", "common"};
		boolean loaded = false;

		for (String path : pathsToTry) {
			try {
				java.io.File dir = new java.io.File(path);
				if (dir.exists() && dir.isDirectory()) {

					Dotenv dotenv = Dotenv.configure()
							.directory(path)
							.filename(".env")
							.ignoreIfMissing()
							.load();

					dotenv.entries().forEach(entry -> {
						System.setProperty(entry.getKey(), entry.getValue());
					});

					log.info("Successfully loaded .env variables from: {}", path);
					loaded = true;
					break;
				}
			} catch (Exception e) {
				log.debug("Could not load .env from {}, trying next path...", path);
			}
		}

		if (!loaded) {
			log.info("No local .env file found. Relying on OS environment variables");
		}
	}
}
