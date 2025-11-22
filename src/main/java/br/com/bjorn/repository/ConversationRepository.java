package br.com.bjorn.repository;

import br.com.bjorn.entity.Conversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @EntityGraph(attributePaths = "knowledgeBase")
    Optional<Conversation> findWithKnowledgeBaseById(Long id);
}
