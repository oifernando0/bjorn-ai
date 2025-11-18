package br.com.bjorn.memory;

import br.com.bjorn.entity.AgentMemory;
import reactor.core.publisher.Mono;

public interface AgentMemoryService {
    Mono<AgentMemory> saveMemory(String description, Integer importance);
}
