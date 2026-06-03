package ru.stepanov.selfcontrol.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IsProperties.class)
public class IsConfiguration {
}
