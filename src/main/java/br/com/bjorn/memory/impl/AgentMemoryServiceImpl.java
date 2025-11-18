package br.com.bjorn.memory.impl;

import br.com.bjorn.entity.AgentMemory;
import br.com.bjorn.memory.AgentMemoryService;
import br.com.bjorn.repository.AgentMemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@Transactional
public class AgentMemoryServiceImpl implements AgentMemoryService {

    private final AgentMemoryRepository repository;

    public AgentMemoryServiceImpl(AgentMemoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<AgentMemory> saveMemory(String description, Integer importance) {
        return Mono.fromCallable(() -> {
            AgentMemory memory = new AgentMemory();
            memory.setDescription(description);
            memory.setImportance(importance);
            return repository.save(memory);
        });
    }
}
