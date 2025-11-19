package br.com.bjorn.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {
    List<KnowledgeChunk> findTop10BySpecialistAndTextContainingIgnoreCase(String specialist, String text);
}
