package br.com.bjorn.service;

import reactor.core.publisher.Mono;

public interface ChatGptService {
    Mono<String> generateAnswer(String systemPrompt, String userPrompt);
}
