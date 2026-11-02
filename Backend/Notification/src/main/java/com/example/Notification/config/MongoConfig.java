package com.example.Notification.config;

import com.example.Notification.shared.enums.NotificationType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

/**
 * MongoDB custom conversions configuration.
 * Registers resilient reading converters for domain enums to prevent unhandled
 * deserialization failures on unknown or legacy BSON values.
 */
@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(new StringToNotificationTypeConverter()));
    }

    @ReadingConverter
    public static class StringToNotificationTypeConverter implements Converter<String, NotificationType> {
        @Override
        public NotificationType convert(String source) {
            return NotificationType.fromString(source);
        }
    }
}
