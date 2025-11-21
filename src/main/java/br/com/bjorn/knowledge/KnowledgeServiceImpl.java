package br.com.bjorn.knowledge;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    public void indexPdf(MultipartFile file, String specialist) {
        String normalizedSpecialist = normalizeSpecialist(specialist);
        if (normalizedSpecialist == null || normalizedSpecialist.isBlank()) {
            throw new IllegalArgumentException("Specialist is required");
        }
        String fileName = file.getOriginalFilename();
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
    public List<KnowledgeChunk> searchRelevantChunks(String specialist, String question, int topK) {
        String normalizedSpecialist = normalizeSpecialist(specialist);
        if (normalizedSpecialist == null || normalizedSpecialist.isBlank()) {
            return Collections.emptyList();
        }
        List<KnowledgeChunk> matches = repository.findTop10BySpecialistAndTextContainingIgnoreCase(normalizedSpecialist, question);
        // FUTURE: replace this simple LIKE query with an embedding/vector similarity search to improve relevance.
        if (matches.isEmpty()) {
            return Collections.emptyList();
        }
        return matches.subList(0, Math.min(matches.size(), topK));
    }

    private String extractText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);
            return rawText == null ? "" : rawText.replaceAll("\\s+", " ").trim();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read PDF file: " + file.getOriginalFilename(), e);
        }
    }

    private List<String> chunk(String text, int size, int overlap) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        List<String> chunks = new ArrayList<>();
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
