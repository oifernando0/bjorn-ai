package br.com.bjorn.service.impl;

import br.com.bjorn.entity.Conversation;
import br.com.bjorn.entity.KnowledgeBase;
import br.com.bjorn.repository.ConversationRepository;
import br.com.bjorn.service.ConversationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository repository;

    public ConversationServiceImpl(ConversationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Conversation createConversation(String title, KnowledgeBase knowledgeBase) {
        Conversation conversation = new Conversation();
        conversation.setTitle(title);
        conversation.setKnowledgeBase(knowledgeBase);
        return repository.save(conversation);
    }

    @Override
    public Conversation getConversation(Long id) {
        return repository.findWithKnowledgeBaseById(id)
                .orElseGet(() -> repository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Conversation not found")));
    }
}
