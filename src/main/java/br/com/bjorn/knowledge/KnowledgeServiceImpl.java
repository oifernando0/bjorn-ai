package br.com.bjorn.knowledge;

import java.io.IOException;
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

@Service
@Transactional
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeServiceImpl.class);
    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int DEFAULT_OVERLAP = 200;
    private static final int MIN_TERM_LENGTH = 3;
    private static final int MAX_RESULTS = 10;

    private final KnowledgeChunkRepository repository;

    public KnowledgeServiceImpl(KnowledgeChunkRepository repository) {
        this.repository = repository;
    }

    @Override
    public void indexPdf(FilePart file, String specialist) {
        String normalizedSpecialist = normalizeSpecialist(specialist);
        if (normalizedSpecialist == null || normalizedSpecialist.isBlank()) {
            throw new IllegalArgumentException("Specialist is required");
        }
        String fileName = file.filename();
        String text = extractText(file);
        List<String> chunks = chunk(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);

        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setSpecialist(normalizedSpecialist);
            chunk.setFileName(fileName);
            chunk.setChunkIndex(i);
            chunk.setText(chunks.get(i));
            repository.save(chunk);
        }

        logger.info("Indexed {} chunks for specialist {} from file {}", chunks.size(), normalizedSpecialist, fileName);
    }

    @Override
    public List<KnowledgeChunk> searchRelevantChunks(String specialist, String question) {
        List<String> keywords = extractKeywords(question);
        if (keywords.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedSpecialist = normalizeSpecialist(specialist);
        List<KnowledgeChunk> candidates = (normalizedSpecialist == null || normalizedSpecialist.isBlank())
                ? repository.findAll()
                : repository.findBySpecialist(normalizedSpecialist);

        List<KnowledgeChunk> ranked = rankChunksByKeywords(candidates, keywords);
        if (!ranked.isEmpty() || normalizedSpecialist == null || normalizedSpecialist.isBlank()) {
            return ranked;
        }

        return rankChunksByKeywords(repository.findAll(), keywords);
    }

    private String extractText(FilePart file) {
        DataBuffer dataBuffer = DataBufferUtils.join(file.content()).block();
        if (dataBuffer == null) {
            throw new IllegalArgumentException("Failed to read PDF file: " + file.filename());
        }

        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);

        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);
            return rawText == null ? "" : rawText.replaceAll("\\s+", " ").trim();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read PDF file: " + file.filename(), e);
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

        String normalized = question.toLowerCase().replaceAll("[\\p{Punct}]", " ");
        String[] tokens = normalized.split("\\s+");

        Set<String> terms = new LinkedHashSet<>();
        for (String token : tokens) {
            if (token.length() >= MIN_TERM_LENGTH) {
                terms.add(token);
            }
        }

        return new ArrayList<>(terms);
    }

    private List<KnowledgeChunk> rankChunksByKeywords(List<KnowledgeChunk> candidates, List<String> keywords) {
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

        List<KnowledgeChunk> ranked = new ArrayList<>();
        for (int i = 0; i < scoredChunks.size() && i < MAX_RESULTS; i++) {
            ranked.add(scoredChunks.get(i).chunk());
        }

        return ranked;
    }

    private int scoreChunk(KnowledgeChunk chunk, List<String> keywords) {
        if (chunk == null || chunk.getText() == null) {
            return 0;
        }

        String chunkText = chunk.getText().toLowerCase();
        int score = 0;
        for (String term : keywords) {
            if (chunkText.contains(term)) {
                score++;
            }
        }
        return score;
    }

    private record ScoredChunk(KnowledgeChunk chunk, int score) {
    }
}
