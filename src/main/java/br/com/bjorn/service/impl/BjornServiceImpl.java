package br.com.bjorn.service.impl;

import br.com.bjorn.dto.MessageResponse;
import br.com.bjorn.entity.Conversation;
import br.com.bjorn.entity.MessageRole;
import br.com.bjorn.knowledge.KnowledgeChunk;
import br.com.bjorn.knowledge.KnowledgeService;
import br.com.bjorn.service.BjornService;
import br.com.bjorn.service.MessageService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class BjornServiceImpl implements BjornService {

    private final MessageService messageService;
    private final KnowledgeService knowledgeService;

    public BjornServiceImpl(MessageService messageService, KnowledgeService knowledgeService) {
        this.messageService = messageService;
        this.knowledgeService = knowledgeService;
    }

    @Override
    public Mono<MessageResponse> handleUserMessage(Conversation conversation, String content) {
        return messageService.saveMessage(conversation, MessageRole.USER, content)
                .then(Mono.fromCallable(() -> knowledgeService.searchRelevantChunks(resolveSpecialist(conversation), content, 5)))
                .flatMap(chunks -> generateAssistantResponse(conversation, content, chunks));
    }

    private Mono<MessageResponse> generateAssistantResponse(Conversation conversation, String content, java.util.List<KnowledgeChunk> chunks) {
        String context = chunks.stream()
                .map(KnowledgeChunk::getText)
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("Nenhum contexto relevante encontrado.");

        String prompt = """
                Você é BJORN – Electrical Specialist.

                Use APENAS as informações abaixo para responder, se possível.
                Se não encontrar nada relevante, responda com base no seu próprio conhecimento,
                mas deixe claro que não encontrou referência direta nos PDFs.

                Contexto extraído dos PDFs:
                %s

                Pergunta do usuário:
                %s
                """.formatted(context, content);

        // TODO: integrate LLM call using the prompt above. The current implementation echoes the prompt as the assistant message.
        return messageService.saveMessage(conversation, MessageRole.ASSISTANT, prompt)
                .map(msg -> new MessageResponse(msg.getId(), msg.getRole().name(), msg.getContent(), msg.getCreatedAt()));
    }

    private String resolveSpecialist(Conversation conversation) {
        // TODO: derive specialist from conversation metadata/knowledge base when available.
        return "ELECTRICAL";
    }
}
