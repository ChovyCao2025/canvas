package org.chovy.canvas.engine.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RuleConfiguration 参与 engine.rule 场景的画布执行引擎处理。
 */
@Configuration
public class RuleConfiguration {

    /**
     * 创建规则解析器 Bean。
     *
     * @param objectMapper JSON 解析器
     * @return 规则解析器
     */
    @Bean
    RuleParser ruleParser(ObjectMapper objectMapper) {
        return new RuleParser(objectMapper);
    }

    /**
     * 创建规则校验器 Bean。
     *
     * @return 规则校验器
     */
    @Bean
    RuleValidator ruleValidator() {
        return new RuleValidator();
    }

    /**
     * 创建规则 SQL 编译器 Bean。
     *
     * @return 规则 SQL 编译器
     */
    @Bean
    RuleSqlCompiler ruleSqlCompiler() {
        return new RuleSqlCompiler();
    }
}
