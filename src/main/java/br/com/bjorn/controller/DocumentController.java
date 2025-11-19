package br.com.bjorn.controller;

import br.com.bjorn.dto.DocumentResponse;
import br.com.bjorn.entity.Conversation;
import br.com.bjorn.entity.Document;
import br.com.bjorn.entity.SourceType;
import br.com.bjorn.service.ConversationService;
import br.com.bjorn.service.DocumentService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/conversations/{conversationId}/documents")
public class DocumentController {

    private final ConversationService conversationService;
    private final DocumentService documentService;

    public DocumentController(ConversationService conversationService, DocumentService documentService) {
        this.conversationService = conversationService;
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<DocumentResponse> uploadDocument(@PathVariable Long conversationId,
                                                 @RequestPart("file") FilePart filePart,
                                                 @RequestPart(value = "sourceType", required = false) String sourceType) {
        Conversation conversation = conversationService.getConversation(conversationId);
        SourceType type = sourceType != null ? SourceType.valueOf(sourceType) : SourceType.OUTRO;
        return documentService.storeDocument(filePart, conversation.getKnowledgeBase(), type)
                .map(this::toResponse);
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(document.getId(), document.getOriginalFilename(), document.getSourceType().name());
    }
}
