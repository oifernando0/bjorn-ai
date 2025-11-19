package br.com.bjorn.service.impl;

import br.com.bjorn.entity.Document;
import br.com.bjorn.entity.KnowledgeBase;
import br.com.bjorn.entity.SourceType;
import br.com.bjorn.repository.DocumentRepository;
import br.com.bjorn.service.DocumentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository repository;
    private final Path storageDirectory;

    public DocumentServiceImpl(DocumentRepository repository,
                               @Value("${bjorn.storage.dir:./storage}") String storageDirectory) {
        this.repository = repository;
        this.storageDirectory = Path.of(storageDirectory);
    }

    @Override
    public Mono<Document> storeDocument(FilePart filePart, KnowledgeBase knowledgeBase, SourceType sourceType) {
        return Mono.fromCallable(() -> {
            if (!Files.exists(storageDirectory)) {
                Files.createDirectories(storageDirectory);
            }
            Path destination = storageDirectory.resolve(filePart.filename());
            // Save file content
            filePart.transferTo(destination).block();

            Document document = new Document();
            document.setKnowledgeBase(knowledgeBase);
            document.setOriginalFilename(filePart.filename());
            document.setMimeType(filePart.headers().getContentType() != null ? filePart.headers().getContentType().toString() : null);
            document.setStoragePath(destination.toAbsolutePath().toString());
            document.setSourceType(sourceType);
            document.setFullText(null); // TODO extract text
            document.setEmbedding(null); // TODO generate embedding
            document.setCreatedAt(OffsetDateTime.now());
            document.setUpdatedAt(OffsetDateTime.now());
            return repository.save(document);
        });
    }
}
