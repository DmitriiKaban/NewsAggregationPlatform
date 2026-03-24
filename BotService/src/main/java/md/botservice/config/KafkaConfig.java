package md.botservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import md.botservice.dto.AnalyticsEventDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.converter.StringJacksonJsonMessageConverter;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, String> defaultConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new StringDeserializer()
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> stringListenerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> notificationListenerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        return createJsonFactory(consumerFactory);
    }

    private ConcurrentKafkaListenerContainerFactory<String, String> createJsonFactory(
            ConsumerFactory<String, String> consumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory);
        factory.setRecordMessageConverter(new StringJacksonJsonMessageConverter());
        return factory;
    }

    @Bean
    @SuppressWarnings("removal")
    public ConcurrentKafkaListenerContainerFactory<String, AnalyticsEventDto> analyticsListenerFactory(
            ObjectMapper objectMapper) {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        org.springframework.kafka.support.serializer.JsonDeserializer<AnalyticsEventDto> jsonDeserializer =
                new org.springframework.kafka.support.serializer.JsonDeserializer<>(AnalyticsEventDto.class, objectMapper);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeHeaders(false);

        org.springframework.kafka.support.serializer.ErrorHandlingDeserializer<AnalyticsEventDto> errorHandlingDeserializer =
                new org.springframework.kafka.support.serializer.ErrorHandlingDeserializer<>(jsonDeserializer);

        DefaultKafkaConsumerFactory<String, AnalyticsEventDto> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandlingDeserializer);

        var factory = new ConcurrentKafkaListenerContainerFactory<String, AnalyticsEventDto>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

}