package br.com.bjorn.service.impl;

import br.com.bjorn.config.OpenAiProperties;
import br.com.bjorn.service.ChatGptService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ChatGptServiceImpl implements ChatGptService {

    private final OpenAiProperties properties;
    private final WebClient webClient;

    public ChatGptServiceImpl(WebClient.Builder builder, OpenAiProperties properties) {
        this.properties = properties;
        this.webClient = builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }

    @Override
    public Mono<String> generateAnswer(String systemPrompt, String userPrompt) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            return Mono.error(new IllegalStateException("OpenAI API key not configured. Set bjorn.openai.api-key or OPENAI_API_KEY."));
        }

        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.getModel(),
                List.of(new ChatMessage("system", systemPrompt), new ChatMessage("user", userPrompt)),
                0.2
        );

        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .map(ChatCompletionResponse::firstMessageContent)
                .flatMap(optional -> optional
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new IllegalStateException("OpenAI response did not include a message."))));
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
}
