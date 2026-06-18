package com.flourish.intelliride.configs;

import com.flourish.intelliride.tools.DriverTools;
import com.flourish.intelliride.tools.RiderTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {
    @Bean
    ToolCallbackProvider mcpToolCallbackProvider(RiderTools riderTools, DriverTools driverTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(riderTools, driverTools)
                .build();
    }
}
