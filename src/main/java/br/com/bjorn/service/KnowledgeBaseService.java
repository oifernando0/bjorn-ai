package br.com.bjorn.service;

import br.com.bjorn.entity.KnowledgeBase;

public interface KnowledgeBaseService {
    KnowledgeBase getDefaultKnowledgeBase();
    KnowledgeBase getById(Long id);
}
