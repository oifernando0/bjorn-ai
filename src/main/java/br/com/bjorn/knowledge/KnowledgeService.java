package br.com.bjorn.knowledge;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeService {
    void indexPdf(MultipartFile file, String specialist);
    List<KnowledgeChunk> searchRelevantChunks(String specialist, String question, int topK);
}
