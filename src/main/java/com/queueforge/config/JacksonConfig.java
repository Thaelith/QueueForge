package com.queueforge.config;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;

@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer pageModuleCustomizer() {
        SimpleModule pageModule = new SimpleModule("PageModule", Version.unknownVersion());
        pageModule.addSerializer(Page.class, new PageSerializer());
        return builder -> builder.modulesToInstall(modules -> modules.add(pageModule));
    }
}
