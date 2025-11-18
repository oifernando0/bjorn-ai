package br.com.bjorn.service;

import br.com.bjorn.entity.Document;
import br.com.bjorn.entity.KnowledgeBase;
import br.com.bjorn.entity.SourceType;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface DocumentService {
    Mono<Document> storeDocument(FilePart filePart, KnowledgeBase knowledgeBase, SourceType sourceType);
}
