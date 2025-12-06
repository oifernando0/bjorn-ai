package br.com.bjorn.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {
    List<KnowledgeChunk> findBySpecialistAndTextContainingIgnoreCase(String specialist, String text);
    List<KnowledgeChunk> findByTextContainingIgnoreCase(String text);
    List<KnowledgeChunk> findBySpecialist(String specialist);

    @Query("""
            SELECT DISTINCT k.fileName
            FROM KnowledgeChunk k
            WHERE (:specialist IS NULL OR k.specialist = :specialist)
            ORDER BY k.fileName
            """)
    List<String> findDistinctFileNamesBySpecialist(@Param("specialist") String specialist);

    @Query(value = """
            SELECT *
            FROM knowledge_chunks
            WHERE (:specialist IS NULL OR specialist = :specialist)
            ORDER BY embedding::vector <-> :queryEmbedding::vector
            LIMIT :limit
            """, nativeQuery = true)
    List<KnowledgeChunk> findTopByEmbeddingSimilarity(@Param("specialist") String specialist,
                                                      @Param("queryEmbedding") String queryEmbedding,
                                                      @Param("limit") int limit);
}
