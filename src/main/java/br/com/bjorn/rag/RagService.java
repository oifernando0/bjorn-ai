package br.com.bjorn.rag;

import br.com.bjorn.entity.Document;
import br.com.bjorn.entity.KnowledgeBase;
import reactor.core.publisher.Flux;

public interface RagService {
    Flux<Document> retrieveRelevantDocuments(KnowledgeBase knowledgeBase, String query, boolean translated);
}
