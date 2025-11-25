package br.com.bjorn.service;

import br.com.bjorn.entity.Conversation;
import br.com.bjorn.entity.Message;
import br.com.bjorn.entity.MessageRole;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageService {
    Mono<Message> saveMessage(Conversation conversation, MessageRole role, String content);
    Flux<Message> getMessages(Conversation conversation);
    Flux<Message> getRecentMessages(Conversation conversation, int limit);
}
