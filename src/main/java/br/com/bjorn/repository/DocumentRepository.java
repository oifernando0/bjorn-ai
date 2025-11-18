package br.com.bjorn.repository;

import br.com.bjorn.entity.Document;
import br.com.bjorn.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByKnowledgeBase(KnowledgeBase knowledgeBase);
}
