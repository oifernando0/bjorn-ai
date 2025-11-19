package br.com.bjorn.service.impl;

import br.com.bjorn.entity.KnowledgeBase;
import br.com.bjorn.repository.KnowledgeBaseRepository;
import br.com.bjorn.service.KnowledgeBaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final String DEFAULT_NAME = "Base Global";
    private final KnowledgeBaseRepository repository;

    public KnowledgeBaseServiceImpl(KnowledgeBaseRepository repository) {
        this.repository = repository;
    }

    @Override
    public KnowledgeBase getDefaultKnowledgeBase() {
        return repository.findByName(DEFAULT_NAME).orElseGet(() -> {
            KnowledgeBase kb = new KnowledgeBase();
            kb.setName(DEFAULT_NAME);
            kb.setDescription("Base padrão de normas, livros e artigos");
            return repository.save(kb);
        });
    }

    @Override
    public KnowledgeBase getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Knowledge base not found"));
    }
}
