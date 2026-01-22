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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI service implementation using Spring AI with Ollama backend.
 * This uses local models (e.g., llama3.2) via Ollama.
 */
public class OllamaAIService implements AIService {
    
    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final String modelName;
    
    public OllamaAIService(ChatClient chatClient, EmbeddingModel embeddingModel, String modelName) {
        this.chatClient = chatClient;
        this.embeddingModel = embeddingModel;
        this.modelName = modelName;
    }
    
    @Override
    public String summarize(String text, int maxLength) {
        String prompt = String.format(
            "Summarize the following text in approximately %d words or less. " +
            "Focus on the key points and main ideas:\n\n%s",
            maxLength, text
        );
        
        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }
    
    @Override
    public String answerQuestion(String text, String question) {
        String prompt = String.format(
            "Based on the following text, answer this question: %s\n\n" +
            "Text:\n%s\n\n" +
            "Answer concisely and accurately. If the answer cannot be found in the text, say so.",
            question, text
        );
        
        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }
    
    @Override
    public Map<String, String> answerQuestions(String text, List<String> questions) {
        Map<String, String> answers = new HashMap<>();
        
        // Build a single prompt with all questions for efficiency
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Based on the following text, answer these questions:\n\n");
        promptBuilder.append("Text:\n").append(text).append("\n\n");
        promptBuilder.append("Questions:\n");
        
        for (int i = 0; i < questions.size(); i++) {
            promptBuilder.append(i + 1).append(". ").append(questions.get(i)).append("\n");
        }
        
        promptBuilder.append("\nProvide numbered answers corresponding to each question.");
        
        String response = chatClient.prompt()
            .user(promptBuilder.toString())
            .call()
            .content();
        
        // Parse the response (simple approach - split by numbers)
        String[] lines = response.split("\n");
        int currentQuestionIdx = 0;
        StringBuilder currentAnswer = new StringBuilder();
        
        for (String line : lines) {
            if (line.matches("^\\d+\\..*") && currentQuestionIdx < questions.size()) {
                if (currentAnswer.length() > 0) {
                    answers.put(questions.get(currentQuestionIdx - 1), currentAnswer.toString().trim());
                    currentAnswer = new StringBuilder();
                }
                currentAnswer.append(line.replaceFirst("^\\d+\\.\\s*", ""));
                currentQuestionIdx++;
            } else if (currentQuestionIdx > 0) {
                currentAnswer.append(" ").append(line);
            }
        }
        
        // Add the last answer
        if (currentQuestionIdx > 0 && currentAnswer.length() > 0) {
            answers.put(questions.get(currentQuestionIdx - 1), currentAnswer.toString().trim());
        }
        
        return answers;
    }
    
    @Override
    public String extractStructuredData(String text, String schema) {
        String prompt = String.format(
            "Extract structured information from the following text according to this JSON schema:\n\n" +
            "Schema:\n%s\n\n" +
            "Text:\n%s\n\n" +
            "Return ONLY valid JSON matching the schema, with no additional text or explanation.",
            schema, text
        );
        
        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }
    
    @Override
    public float[] generateEmbedding(String text) {
        if (embeddingModel == null) {
            throw new UnsupportedOperationException("Embedding model not configured");
        }
        
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        
        if (response.getResults().isEmpty()) {
            return new float[0];
        }
        
        // Get the embedding output - Spring AI returns float[] directly
        float[] embedding = response.getResults().get(0).getOutput();
        
        return embedding;
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // Try a simple ping to verify the model is available
            chatClient.prompt()
                .user("Test")
                .call()
                .content();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getServiceName() {
        return "ollama-" + modelName;
    }
}
