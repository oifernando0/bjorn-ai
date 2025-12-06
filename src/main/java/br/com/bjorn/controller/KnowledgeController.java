package br.com.bjorn.controller;

import br.com.bjorn.knowledge.KnowledgeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping(value = "/{specialist}/docs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Void>> uploadDocs(@PathVariable String specialist, @RequestPart("files") List<FilePart> files) {
        if (files == null || files.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        String normalizedSpecialist = specialist == null ? null : specialist.toUpperCase();
        return Flux.fromIterable(files)
                .flatMap(file -> knowledgeService.indexPdf(file, normalizedSpecialist))
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
