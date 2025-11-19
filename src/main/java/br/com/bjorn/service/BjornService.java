package br.com.bjorn.service;

import br.com.bjorn.dto.MessageResponse;
import br.com.bjorn.entity.Conversation;
import reactor.core.publisher.Mono;

public interface BjornService {
    Mono<MessageResponse> handleUserMessage(Conversation conversation, String content);
}
