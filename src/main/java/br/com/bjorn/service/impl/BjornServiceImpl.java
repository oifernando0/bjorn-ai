package br.com.bjorn.service.impl;

import br.com.bjorn.dto.MessageResponse;
import br.com.bjorn.entity.Conversation;
import br.com.bjorn.entity.MessageRole;
import br.com.bjorn.knowledge.KnowledgeChunk;
import br.com.bjorn.knowledge.KnowledgeService;
import br.com.bjorn.service.ChatGptService;
import br.com.bjorn.service.BjornService;
import br.com.bjorn.service.MessageService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class BjornServiceImpl implements BjornService {

    private final MessageService messageService;
    private final KnowledgeService knowledgeService;
    private final ChatGptService chatGptService;

    private static final String SYSTEM_PROMPT = """
            Você é BJORN – Electrical Specialist.

            Responda sempre em português.
            Use APENAS as informações do contexto fornecido para responder.
            Se não encontrar referência direta, explique que não encontrou a informação nos materiais disponíveis
            e então ofereça a melhor orientação possível.
            """;

    public BjornServiceImpl(MessageService messageService, KnowledgeService knowledgeService, ChatGptService chatGptService) {
        this.messageService = messageService;
        this.knowledgeService = knowledgeService;
        this.chatGptService = chatGptService;
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

        String userPrompt = """
                Contexto extraído dos PDFs ou documentos indexados:
                %s

                Pergunta do usuário:
                %s
                """.formatted(context, content);

        return chatGptService.generateAnswer(SYSTEM_PROMPT, userPrompt)
                .flatMap(answer -> messageService.saveMessage(conversation, MessageRole.ASSISTANT, answer))
                .map(msg -> new MessageResponse(msg.getId(), msg.getRole().name(), msg.getContent(), msg.getCreatedAt()));
    }

    private String resolveSpecialist(Conversation conversation) {
        // TODO: derive specialist from conversation metadata/knowledge base when available.
        return "ELECTRICAL";
    }
}
