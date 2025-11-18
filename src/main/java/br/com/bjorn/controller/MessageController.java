package br.com.bjorn.controller;

import br.com.bjorn.dto.MessageRequest;
import br.com.bjorn.dto.MessageResponse;
import br.com.bjorn.entity.Conversation;
import br.com.bjorn.service.BjornService;
import br.com.bjorn.service.ConversationService;
import br.com.bjorn.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
public class MessageController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final BjornService bjornService;

    public MessageController(ConversationService conversationService, MessageService messageService, BjornService bjornService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.bjornService = bjornService;
    }

    @PostMapping
    public Mono<MessageResponse> sendMessage(@PathVariable Long conversationId, @Valid @RequestBody MessageRequest request) {
        Conversation conversation = conversationService.getConversation(conversationId);
        return bjornService.handleUserMessage(conversation, request.getContent());
    }

    @GetMapping
    public Flux<MessageResponse> listMessages(@PathVariable Long conversationId) {
        Conversation conversation = conversationService.getConversation(conversationId);
        return messageService.getMessages(conversation)
                .map(message -> new MessageResponse(message.getId(), message.getRole().name(), message.getContent(), message.getCreatedAt()));
    }
}
