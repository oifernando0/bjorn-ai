package br.com.bjorn.service.impl;

import br.com.bjorn.dto.MessageResponse;
import br.com.bjorn.entity.Conversation;
import br.com.bjorn.entity.Document;
import br.com.bjorn.entity.MessageRole;
import br.com.bjorn.rag.RagService;
import br.com.bjorn.service.BjornService;
import br.com.bjorn.service.MessageService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class BjornServiceImpl implements BjornService {

    private final RagService ragService;
    private final MessageService messageService;

    public BjornServiceImpl(RagService ragService, MessageService messageService) {
        this.ragService = ragService;
        this.messageService = messageService;
    }

    @Override
    public Mono<MessageResponse> handleUserMessage(Conversation conversation, String content) {
        return messageService.saveMessage(conversation, MessageRole.USER, content)
                .thenMany(ragService.retrieveRelevantDocuments(conversation.getKnowledgeBase(), content, false))
                .collectList()
                .flatMap(documents -> generateAssistantResponse(conversation, content, documents));
    }

    private Mono<MessageResponse> generateAssistantResponse(Conversation conversation, String content, java.util.List<Document> documents) {
        StringBuilder builder = new StringBuilder();
        builder.append("BJORN – Electrical Specialist\n");
        builder.append("Pergunta: ").append(content).append("\n");
        builder.append("Referências locais disponíveis: ");
        if (documents.isEmpty()) {
            builder.append("nenhuma encontrada (TODO: fallback web / tradução)");
        } else {
            documents.forEach(doc -> builder.append("- ").append(doc.getShortCitation() != null ? doc.getShortCitation() : doc.getOriginalFilename()).append("\n"));
        }
        String answer = builder.toString();
        return messageService.saveMessage(conversation, MessageRole.ASSISTANT, answer)
                .map(msg -> new MessageResponse(msg.getId(), msg.getRole().name(), msg.getContent(), msg.getCreatedAt()));
    }
}
