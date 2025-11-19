package br.com.bjorn.repository;

import br.com.bjorn.entity.MessageDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageDocumentRepository extends JpaRepository<MessageDocument, Long> {
}
