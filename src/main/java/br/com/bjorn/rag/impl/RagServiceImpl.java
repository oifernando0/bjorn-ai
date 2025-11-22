package br.com.bjorn.rag.impl;

import br.com.bjorn.entity.Document;
import br.com.bjorn.entity.KnowledgeBase;
import br.com.bjorn.knowledge.KnowledgeChunk;
import br.com.bjorn.knowledge.KnowledgeChunkRepository;
import br.com.bjorn.rag.RagService;
import br.com.bjorn.service.ChatGptService;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class RagServiceImpl implements RagService {

    private static final int SEMANTIC_SHORTLIST = 50;

    private final KnowledgeChunkRepository repository;
    private final ChatGptService chatGptService;

    public RagServiceImpl(KnowledgeChunkRepository repository, ChatGptService chatGptService) {
        this.repository = repository;
        this.chatGptService = chatGptService;
    }

    @Override
    public Flux<Document> retrieveRelevantDocuments(KnowledgeBase knowledgeBase, String query, boolean translated) {
        // TODO: implement document retrieval when documents are indexed separately
        return Flux.empty();
    }

    @Override
    public List<KnowledgeChunk> retrieveRelevantChunks(String specialist, String question, int maxResults) {
        float[] queryEmbedding = generateQueryEmbedding(question);
        if (queryEmbedding == null) {
            return Collections.emptyList();
        }

        int limit = Math.max(maxResults, SEMANTIC_SHORTLIST);
        List<KnowledgeChunk> candidates = repository.findTopByEmbeddingSimilarity(specialist, queryEmbedding, limit);
        // TODO: consider applying a semantic similarity threshold once distance values are available from the query
        return candidates == null ? Collections.emptyList() : candidates;
    }

    private float[] generateQueryEmbedding(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }
        try {
            return chatGptService.generateEmbedding(question).block();
        } catch (Exception ex) {
            return null;
        }
    }
}
