package br.com.bjorn.repository;

import br.com.bjorn.entity.AgentMemory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentMemoryRepository extends JpaRepository<AgentMemory, Long> {
}
