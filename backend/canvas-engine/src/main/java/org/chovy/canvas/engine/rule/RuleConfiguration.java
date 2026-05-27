package org.chovy.canvas.engine.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuleConfiguration {

    @Bean
    RuleParser ruleParser(ObjectMapper objectMapper) {
        return new RuleParser(objectMapper);
    }

    @Bean
    RuleValidator ruleValidator() {
        return new RuleValidator();
    }

    @Bean
    RuleSqlCompiler ruleSqlCompiler() {
        return new RuleSqlCompiler();
    }
}
