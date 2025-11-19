package br.com.bjorn.service.impl;

import br.com.bjorn.entity.Conversation;
import br.com.bjorn.entity.Message;
import br.com.bjorn.entity.MessageRole;
import br.com.bjorn.repository.MessageRepository;
import br.com.bjorn.service.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository repository;

    public MessageServiceImpl(MessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Message> saveMessage(Conversation conversation, MessageRole role, String content) {
        return Mono.fromCallable(() -> {
            Message message = new Message();
            message.setConversation(conversation);
            message.setRole(role);
            message.setContent(content);
            return repository.save(message);
        });
    }

    @Override
    public Flux<Message> getMessages(Conversation conversation) {
        return Flux.fromIterable(repository.findByConversationOrderByCreatedAtAsc(conversation));
    }
}
