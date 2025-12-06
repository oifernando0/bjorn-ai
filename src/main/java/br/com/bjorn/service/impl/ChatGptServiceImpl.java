package br.com.bjorn.service.impl;

import br.com.bjorn.config.OpenAiProperties;
import br.com.bjorn.service.ChatGptService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ChatGptServiceImpl implements ChatGptService {

    private static final Logger logger = LoggerFactory.getLogger(ChatGptServiceImpl.class);

    private final OpenAiProperties properties;
    private final WebClient webClient;

    public ChatGptServiceImpl(WebClient.Builder builder, OpenAiProperties properties) {
        this.properties = properties;
        this.webClient = builder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public Mono<String> generateAnswer(String systemPrompt, String userPrompt) {
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return Mono.error(new IllegalStateException(
                    "OpenAI API key not configured. Set bjorn.openai.api-key or OPENAI_API_KEY."));
        }

        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.getModel(),
                List.of(new ChatMessage("system", systemPrompt), new ChatMessage("user", userPrompt)),
                0.2
        );

        return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .map(ChatCompletionResponse::firstMessageContent)
                .flatMap(optional -> optional
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new IllegalStateException("OpenAI response did not include a message."))));
    }

    @Override
    public Mono<float[]> generateEmbedding(String text) {
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return Mono.error(new IllegalStateException(
                    "OpenAI API key not configured. Set bjorn.openai.api-key or OPENAI_API_KEY."));
        }

        logger.info("Generating embedding using model '{}' (inputLength={})", properties.getEmbeddingModel(),
                text == null ? 0 : text.length());

        EmbeddingRequest request = new EmbeddingRequest(properties.getEmbeddingModel(), text);

        return webClient.post()
                .uri("/embeddings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .flatMap(response -> {
                    if (response == null || response.data == null || response.data.isEmpty()) {
                        return Mono.error(new IllegalStateException("OpenAI embedding response is empty."));
                    }
                    List<Double> values = response.data.get(0).embedding;
                    if (values == null || values.isEmpty()) {
                        return Mono.error(new IllegalStateException("OpenAI embedding vector missing."));
                    }
                    float[] vector = new float[values.size()];
                    for (int i = 0; i < values.size(); i++) {
                        vector[i] = values.get(i).floatValue();
                    }
                    logger.info("Embedding generated successfully (dimensions={})", vector.length);
                    return Mono.just(vector);
                });
    }

    private record ChatCompletionRequest(String model, List<ChatMessage> messages, double temperature) { }

    private record ChatMessage(String role, String content) { }

    private record ChatCompletionResponse(List<Choice> choices) {
        private Optional<String> firstMessageContent() {
            if (choices == null) {
                return Optional.empty();
            }
            return choices.stream()
                    .map(Choice::message)
                    .filter(Objects::nonNull)
                    .map(ChatMessage::content)
                    .filter(Objects::nonNull)
                    .findFirst();
        }

        private record Choice(ChatMessage message) { }
    }

    private record EmbeddingRequest(String model, String input) { }

    private record EmbeddingResponse(List<EmbeddingData> data) { }

    private record EmbeddingData(List<Double> embedding) { }
}
