package br.com.bjorn.knowledge;

import org.springframework.http.codec.multipart.FilePart;

import java.util.List;

public interface KnowledgeService {
    void indexPdf(FilePart file, String specialist);
    List<KnowledgeChunk> searchRelevantChunks(String specialist, String question, int topK);
}
