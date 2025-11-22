package br.com.bjorn.knowledge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        String normalizedSpecialist = normalizeSpecialist(specialist);
        if (normalizedSpecialist == null || normalizedSpecialist.isBlank()) {
            return findMatchesAcrossSpecialists(question);
        }
        List<KnowledgeChunk> matches = repository.findBySpecialistAndTextContainingIgnoreCase(normalizedSpecialist, question);
        // FUTURE: replace this simple LIKE query with an embedding/vector similarity search to improve relevance.
        if (!matches.isEmpty()) {
            return matches;
        }

        return findMatchesAcrossSpecialists(question);
    }

    private List<KnowledgeChunk> findMatchesAcrossSpecialists(String question) {
        return repository.findByTextContainingIgnoreCase(question);
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
}
