package md.botservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper fasterXmlObjectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }
}
