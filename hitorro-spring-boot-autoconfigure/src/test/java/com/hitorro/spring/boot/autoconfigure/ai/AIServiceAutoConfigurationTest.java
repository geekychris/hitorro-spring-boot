/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.spring.boot.autoconfigure.ai;

import com.hitorro.basedms.transformer.ai.AIService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring AI 2.x wiring: the Ollama starter's beans plus HiTorro's AIService.
 * Nothing here calls Ollama; the beans are only created.
 */
class AIServiceAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RestClientAutoConfiguration.class,
                    WebClientAutoConfiguration.class,
                    ToolCallingAutoConfiguration.class,
                    OllamaApiAutoConfiguration.class,
                    OllamaChatAutoConfiguration.class,
                    OllamaEmbeddingAutoConfiguration.class,
                    ChatClientAutoConfiguration.class,
                    AIServiceAutoConfiguration.class))
            .withPropertyValues(
                    "spring.ai.ollama.chat.model=llama3.2",
                    "spring.ai.ollama.embedding.model=nomic-embed-text");

    @Test
    void createsAiServiceAroundOllamaChatModel() {
        runner.withPropertyValues("hitorro.ai.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AIService.class);
            assertThat(context).hasSingleBean(ChatClient.Builder.class);
            assertThat(context).hasSingleBean(EmbeddingModel.class);
            assertThat(context.getBean(ChatModel.class)).isInstanceOf(OllamaChatModel.class);
            assertThat(context.getBean(ChatModel.class).getDefaultOptions().getModel()).isEqualTo("llama3.2");
            assertThat(context.getBean(AIService.class)).isInstanceOf(OllamaAIService.class);
        });
    }

    @Test
    void deprecatedOptionsModelPropertyStillSelectsTheModel() {
        // SPRING_AI_OLLAMA_CHAT_OPTIONS_MODEL binds to spring.ai.ollama.chat.options.model
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        RestClientAutoConfiguration.class,
                        WebClientAutoConfiguration.class,
                        ToolCallingAutoConfiguration.class,
                        OllamaApiAutoConfiguration.class,
                        OllamaChatAutoConfiguration.class))
                .withPropertyValues("spring.ai.ollama.chat.options.model=qwen2.5")
                .run(context -> assertThat(context.getBean(ChatModel.class).getDefaultOptions().getModel())
                        .isEqualTo("qwen2.5"));
    }

    @Test
    void aiServiceIsOffByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(AIService.class);
        });
    }
}
