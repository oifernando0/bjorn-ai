package br.com.bjorn.knowledge;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.util.List;

public interface KnowledgeService {
    Mono<Void> indexPdf(FilePart file, String specialist);
    List<KnowledgeChunk> searchRelevantChunks(String specialist, String question);
    int getLastMaxScore();
    int getMinAcceptableScore();
}
