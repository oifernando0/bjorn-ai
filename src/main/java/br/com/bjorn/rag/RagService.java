package br.com.bjorn.rag;

import br.com.bjorn.entity.Document;
import br.com.bjorn.entity.KnowledgeBase;
import br.com.bjorn.knowledge.KnowledgeChunk;
import reactor.core.publisher.Flux;
import java.util.List;

public interface RagService {
    Flux<Document> retrieveRelevantDocuments(KnowledgeBase knowledgeBase, String query, boolean translated);
    List<KnowledgeChunk> retrieveRelevantChunks(String specialist, String question, int maxResults);
}
