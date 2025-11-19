package br.com.bjorn.rag.impl;

import br.com.bjorn.entity.Document;
import br.com.bjorn.entity.KnowledgeBase;
import br.com.bjorn.rag.RagService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class RagServiceImpl implements RagService {
    @Override
    public Flux<Document> retrieveRelevantDocuments(KnowledgeBase knowledgeBase, String query, boolean translated) {
        // TODO: implement embedding similarity search using pgvector
        return Flux.empty();
    }
}
