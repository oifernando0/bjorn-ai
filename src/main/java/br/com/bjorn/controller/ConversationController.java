package br.com.bjorn.controller;

import br.com.bjorn.dto.CreateConversationRequest;
import br.com.bjorn.dto.CreateConversationResponse;
import br.com.bjorn.entity.KnowledgeBase;
import br.com.bjorn.service.ConversationService;
import br.com.bjorn.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final KnowledgeBaseService knowledgeBaseService;

    public ConversationController(ConversationService conversationService, KnowledgeBaseService knowledgeBaseService) {
        this.conversationService = conversationService;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping
    public Mono<CreateConversationResponse> createConversation(@Valid @RequestBody CreateConversationRequest request) {
        return Mono.fromCallable(() -> {
            KnowledgeBase knowledgeBase = request.getKnowledgeBaseId() != null
                    ? knowledgeBaseService.getById(request.getKnowledgeBaseId())
                    : knowledgeBaseService.getDefaultKnowledgeBase();
            return conversationService.createConversation(request.getTitle(), knowledgeBase);
        }).map(conversation -> new CreateConversationResponse(conversation.getId()));
    }
}
