package br.com.bjorn.service;

import br.com.bjorn.entity.Conversation;
import br.com.bjorn.entity.KnowledgeBase;

public interface ConversationService {
    Conversation createConversation(String title, KnowledgeBase knowledgeBase);
    Conversation getConversation(Long id);
}
