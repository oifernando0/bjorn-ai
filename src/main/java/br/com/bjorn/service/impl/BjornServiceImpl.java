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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class BjornServiceImpl implements BjornService {

    private final MessageService messageService;
    private final KnowledgeService knowledgeService;
    private final ChatGptService chatGptService;

    private static final String SYSTEM_PROMPT = """
Você é Bjorn, um especialista técnico.
- Quando receber contexto com trechos de materiais, responda APENAS com base nesses trechos.
- Quando o prompt informar que não há contexto, responda de forma geral, mas deixe isso explícito na resposta.
- Nunca invente nomes de arquivos nem referências: o backend adicionará as referências reais na resposta.
""";

    private static final String DEFAULT_SPECIALIST = "ELECTRICAL";
    private static final String DEFAULT_KNOWLEDGE_BASE_NAME = "Base Global";

    public BjornServiceImpl(MessageService messageService, KnowledgeService knowledgeService, ChatGptService chatGptService) {
        this.messageService = messageService;
        this.knowledgeService = knowledgeService;
        this.chatGptService = chatGptService;
    }

    @Override
    public Mono<MessageResponse> handleUserMessage(Conversation conversation, String content) {
        String specialist = resolveSpecialist(conversation);
        return messageService.saveMessage(conversation, MessageRole.USER, content)
                .then(Mono.fromCallable(() -> knowledgeService.searchRelevantChunks(specialist, content)))
                .map(chunks -> chunks.isEmpty() && !DEFAULT_SPECIALIST.equalsIgnoreCase(specialist)
                        ? knowledgeService.searchRelevantChunks(DEFAULT_SPECIALIST, content)
                        : chunks)
                .flatMap(chunks -> generateAssistantResponse(conversation, content, chunks));
    }

    private Mono<MessageResponse> generateAssistantResponse(Conversation conversation, String content, List<KnowledgeChunk> chunks) {
        List<KnowledgeChunk> safeChunks = chunks == null ? List.of() : chunks;
        boolean hasContext = !safeChunks.isEmpty();
        Set<String> fileNames = extractFileNames(safeChunks);

        String userPrompt;
        if (hasContext) {
            String context = buildContextWithReferences(safeChunks);
            userPrompt = """
                    Contexto extraído dos PDFs ou documentos indexados:
                    %s

                    Tarefa:
                    - Responda APENAS com base nos trechos acima.
                    - Use linguagem clara e didática em português.
                    - Consolide as informações se vários trechos falarem do mesmo assunto.
                    - NÃO use conhecimento externo.
                    - Ao final da resposta, haverá uma seção "Referências consultadas" adicionada pelo backend.

                    Pergunta do usuário:
                    %s
                    """.formatted(context, content);
        } else {
            userPrompt = """
                    Os materiais de referência cadastrados (PDFs) não possuem trechos relevantes para responder à pergunta abaixo.

                    Tarefa:
                    - Explique de forma geral em português, usando seu conhecimento, mas deixe CLARO no início da resposta que não foi encontrada informação nos materiais de referência.
                    - Exemplo de início: "Não encontrei informações sobre esse tema nos materiais cadastrados, mas, de forma geral, ..."
                    - Não invente referências de arquivos, pois não há trechos mapeados.

                    Pergunta do usuário:
                    %s
                    """.formatted(content);
        }

        return chatGptService.generateAnswer(SYSTEM_PROMPT, userPrompt)
                .map(answer -> hasContext ? appendReferences(answer, fileNames) : answer)
                .flatMap(finalAnswer -> messageService.saveMessage(conversation, MessageRole.ASSISTANT, finalAnswer))
                .map(msg -> new MessageResponse(msg.getId(), msg.getRole().name(), msg.getContent(), msg.getCreatedAt()));
    }

    private Set<String> extractFileNames(List<KnowledgeChunk> chunks) {
        Set<String> fileNames = new LinkedHashSet<>();
        for (KnowledgeChunk chunk : chunks) {
            if (chunk != null) {
                String name = chunk.getFileName();
                if (name != null && !name.isBlank()) {
                    fileNames.add(name);
                }
            }
        }
        return fileNames;
    }

    private String buildContextWithReferences(List<KnowledgeChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("Trechos relevantes dos materiais (cada trecho traz sua fonte):\n\n");
        int idx = 1;
        for (KnowledgeChunk c : chunks) {
            if (c == null) {
                continue;
            }
            sb.append("[").append(idx).append("] ")
                    .append("Fonte: ").append(c.getFileName())
                    .append(" | Chunk: ").append(c.getChunkIndex())
                    .append("\n");
            sb.append(c.getText()).append("\n\n");
            idx++;
        }
        return sb.toString();
    }

    private String appendReferences(String answer, Set<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return answer;
        }

        StringBuilder sb = new StringBuilder(answer);
        sb.append("\n\nReferências consultadas:\n");
        for (String name : fileNames) {
            sb.append("- ").append(name).append("\n");
        }
        return sb.toString();
    }

    private String resolveSpecialist(Conversation conversation) {
        if (conversation != null && conversation.getKnowledgeBase() != null) {
            String kbName = conversation.getKnowledgeBase().getName();
            if (kbName != null && !kbName.isBlank()) {
                if (kbName.equalsIgnoreCase(DEFAULT_KNOWLEDGE_BASE_NAME)) {
                    return DEFAULT_SPECIALIST;
                }
                return kbName.trim().toUpperCase();
            }
        }

        return DEFAULT_SPECIALIST;
    }
}
