package br.com.bjorn.knowledge;

import br.com.bjorn.rag.RagService;
import br.com.bjorn.service.ChatGptService;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Transactional
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeServiceImpl.class);
    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int DEFAULT_OVERLAP = 200;
    private static final int MIN_TERM_LENGTH = 3;
    private static final int MAX_RESULTS = 10;
    private static final int MIN_ACCEPTABLE_SCORE = 2;
    private static final int TECHNICAL_TERM_WEIGHT = 3;
    private static final int SEMANTIC_CANDIDATES = 50;

    private static final Set<String> STOPWORDS = Set.of(
            "de", "da", "do", "das", "dos", "em", "no", "na", "nos", "nas", "o", "a", "os", "as",
            "que", "para", "com", "por", "uma", "um", "uns", "umas"
    );

    private static final Set<String> TECHNICAL_TERMS = Set.of(
            "resistor", "resistencia", "capacitor", "indutor", "corrente", "tensao", "voltagem", "potencia",
            "circuito", "transformador", "condutor", "frequencia", "impedancia", "amperagem", "ohm"
    );

    private final KnowledgeChunkRepository repository;
    private final ChatGptService chatGptService;
    private final RagService ragService;
    private volatile int lastMaxScore = 0;

    public KnowledgeServiceImpl(KnowledgeChunkRepository repository, ChatGptService chatGptService, RagService ragService) {
        this.repository = repository;
        this.chatGptService = chatGptService;
        this.ragService = ragService;
    }

    @Override
    public Mono<Void> indexPdf(FilePart file, String specialist) {
        String normalizedSpecialist = normalizeSpecialist(specialist);
        if (normalizedSpecialist == null || normalizedSpecialist.isBlank()) {
            return Mono.error(new IllegalArgumentException("Specialist is required"));
        }
        String fileName = file.filename();

        logger.info("Starting indexing for file {} and specialist {}", fileName, normalizedSpecialist);

        return DataBufferUtils.join(file.content())
                .publishOn(Schedulers.boundedElastic())
                .flatMap(dataBuffer -> Mono.fromCallable(() -> extractText(dataBuffer, fileName)))
                .map(text -> {
                    List<String> chunks = chunk(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
                    for (int i = 0; i < chunks.size(); i++) {
                        KnowledgeChunk chunk = new KnowledgeChunk();
                        chunk.setSpecialist(normalizedSpecialist);
                        chunk.setFileName(fileName);
                        chunk.setChunkIndex(i);
                        chunk.setText(chunks.get(i));
                        chunk.setEmbedding(generateEmbeddingSafe(chunks.get(i)));
                        repository.save(chunk);
                    }
                    return chunks.size();
                })
                .doOnNext(count -> logger.info("Indexed {} chunks for specialist {} from file {}", count, normalizedSpecialist, fileName))
                .doOnTerminate(() -> logger.info("Finished indexing for file {} and specialist {}", fileName, normalizedSpecialist))
                .then();
    }

    @Override
    public List<KnowledgeChunk> searchRelevantChunks(String specialist, String question) {
        lastMaxScore = 0;
        List<String> keywords = extractKeywords(question);
        if (keywords.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedSpecialist = normalizeSpecialist(specialist);
        List<KnowledgeChunk> candidates = retrieveCandidates(normalizedSpecialist, question);
        List<ScoredChunk> scored = scoreChunks(candidates, keywords);

        if (scored.isEmpty() && normalizedSpecialist != null && !normalizedSpecialist.isBlank()) {
            List<ScoredChunk> fallback = scoreChunks(repository.findAll(), keywords);
            return finalizeRanking(fallback);
        }

        return finalizeRanking(scored);
    }

    @Override
    public int getLastMaxScore() {
        return lastMaxScore;
    }

    @Override
    public int getMinAcceptableScore() {
        return MIN_ACCEPTABLE_SCORE;
    }

    private List<KnowledgeChunk> retrieveCandidates(String specialist, String question) {
        List<KnowledgeChunk> semantic = ragService.retrieveRelevantChunks(specialist, question, SEMANTIC_CANDIDATES);
        if (semantic != null && !semantic.isEmpty()) {
            return semantic;
        }
        return (specialist == null || specialist.isBlank())
                ? repository.findAll()
                : repository.findBySpecialist(specialist);
    }

    private List<ScoredChunk> scoreChunks(List<KnowledgeChunk> candidates, List<String> keywords) {
        if (candidates == null || candidates.isEmpty() || keywords.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScoredChunk> scoredChunks = new ArrayList<>();
        for (KnowledgeChunk chunk : candidates) {
            int score = scoreChunk(chunk, keywords);
            if (score > 0) {
                scoredChunks.add(new ScoredChunk(chunk, score));
            }
        }

        scoredChunks.sort(Comparator.comparingInt(ScoredChunk::score).reversed());
        return scoredChunks;
    }

    private List<KnowledgeChunk> finalizeRanking(List<ScoredChunk> scoredChunks) {
        if (scoredChunks == null || scoredChunks.isEmpty()) {
            lastMaxScore = 0;
            return Collections.emptyList();
        }

        lastMaxScore = scoredChunks.get(0).score();
        if (lastMaxScore < MIN_ACCEPTABLE_SCORE) {
            return Collections.emptyList();
        }

        List<KnowledgeChunk> ranked = new ArrayList<>();
        for (int i = 0; i < scoredChunks.size() && i < MAX_RESULTS; i++) {
            ranked.add(scoredChunks.get(i).chunk());
        }
        return ranked;
    }

    private String extractText(DataBuffer dataBuffer, String fileName) {
        if (dataBuffer == null) {
            throw new IllegalArgumentException("Failed to read PDF file: " + fileName);
        }

        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);

        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);
            return rawText == null ? "" : rawText.replaceAll("\\s+", " ").trim();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read PDF file: " + fileName, e);
        } finally {
            DataBufferUtils.release(dataBuffer);
        }
    }

    private List<String> chunk(String text, int size, int overlap) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        List<String> chunks;
        chunks = new ArrayList<>();
        int start = 0;
        int step = Math.max(1, size - overlap);

        while (start < text.length()) {
            int end = Math.min(start + size, text.length());
            String part = text.substring(start, end).trim();
            if (!part.isBlank()) {
                chunks.add(part);
            }
            if (end == text.length()) {
                break;
            }
            start += step;
        }
        return chunks;
    }

    private String normalizeSpecialist(String specialist) {
        return specialist == null ? null : specialist.toUpperCase();
    }

    private List<String> extractKeywords(String question) {
        if (question == null || question.isBlank()) {
            return Collections.emptyList();
        }

        String normalized = normalizeText(question).replaceAll("[\\p{Punct}]", " ");
        String[] tokens = normalized.split("\\s+");

        Set<String> terms = new LinkedHashSet<>();
        for (String token : tokens) {
            if (token.length() >= MIN_TERM_LENGTH && !STOPWORDS.contains(token)) {
                terms.add(token);
            }
        }

        return new ArrayList<>(terms);
    }

    private int scoreChunk(KnowledgeChunk chunk, List<String> keywords) {
        if (chunk == null || chunk.getText() == null) {
            return 0;
        }

        String chunkText = normalizeText(chunk.getText());
        int score = 0;
        for (String term : keywords) {
            if (chunkText.contains(term)) {
                score += TECHNICAL_TERMS.contains(term) ? TECHNICAL_TERM_WEIGHT : 1;
            }
        }
        return score;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase();
    }

    private float[] generateEmbeddingSafe(String text) {
        try {
            return chatGptService.generateEmbedding(text).block();
        } catch (Exception ex) {
            logger.warn("Failed to generate embedding for chunk", ex);
            // TODO: handle retries or fallback when embedding generation fails
            return null;
        }
    }

    private record ScoredChunk(KnowledgeChunk chunk, int score) {
    }
}
