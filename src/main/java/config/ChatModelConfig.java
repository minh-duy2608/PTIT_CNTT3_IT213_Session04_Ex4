package config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelConfig {

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiChatModel openAiChatModel) {
        return openAiChatModel;
    }
}