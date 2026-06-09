package com.example.campusrag.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.campusrag.common.BusinessException;
import com.example.campusrag.domain.DocumentStatus;
import com.example.campusrag.domain.KnowledgeChunk;
import com.example.campusrag.domain.KnowledgeDocument;
import com.example.campusrag.domain.UserAccount;
import com.example.campusrag.repository.KnowledgeChunkRepository;
import com.example.campusrag.repository.KnowledgeDocumentRepository;
import com.example.campusrag.service.VectorStore.VectorChunk;

@Service
public class DocumentService {
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final Tika tika = new Tika();

    public DocumentService(
            KnowledgeDocumentRepository documentRepository,
            KnowledgeChunkRepository chunkRepository,
            TextChunker textChunker,
            EmbeddingService embeddingService,
            VectorStore vectorStore) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.textChunker = textChunker;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    @Transactional
    public KnowledgeDocument upload(MultipartFile file, String department, String visibility, UserAccount user) {
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename() == null ? "knowledge.txt" : file.getOriginalFilename();
        String extension = extension(originalName);
        KnowledgeDocument document = newDocument(originalName, extension, department, visibility, user);
        try {
            Path storageDir = Path.of("storage", "uploads");
            Files.createDirectories(storageDir);
            Path path = storageDir.resolve(UUID.randomUUID() + "-" + originalName);
            file.transferTo(path);
            document.setStoragePath(path.toString());
            document = documentRepository.save(document);
            String text = parse(file.getInputStream(), originalName);
            return processText(document, text);
        } catch (Exception exception) {
            document.setStatus(DocumentStatus.FAILED);
            document.setFailureReason(exception.getMessage());
            document.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(document);
            throw new BusinessException("文档处理失败：" + exception.getMessage());
        }
    }

    @Transactional
    public KnowledgeDocument ingestText(String originalName, String text, String department, String visibility, UserAccount user) {
        KnowledgeDocument document = newDocument(originalName, extension(originalName), department, visibility, user);
        document = documentRepository.save(document);
        return processText(document, text);
    }

    @Transactional
    public KnowledgeDocument createTextDocument(String title, String text, String department, String visibility, UserAccount user) {
        String name = title == null || title.isBlank() ? "手动录入资料.md" : title;
        if (!name.contains(".")) {
            name = name + ".md";
        }
        return ingestText(name, text, department, visibility, user);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocument> listDocuments() {
        return documentRepository.findAll().stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeDocument getDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("文档不存在"));
    }

    @Transactional(readOnly = true)
    public List<KnowledgeChunk> chunks(Long documentId) {
        return chunkRepository.findByDocumentIdOrderByChunkNoAsc(documentId);
    }

    @Transactional
    public KnowledgeDocument rebuild(Long documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("文档不存在"));
        String text;
        try {
            if (document.getStoragePath() != null && !document.getStoragePath().isBlank()) {
                Path path = Path.of(document.getStoragePath());
                try (InputStream stream = Files.newInputStream(path)) {
                    text = parse(stream, document.getOriginalName());
                }
            } else {
                text = chunkRepository.findByDocumentIdOrderByChunkNoAsc(documentId).stream()
                        .map(KnowledgeChunk::getContent)
                        .reduce("", (left, right) -> left + "\n\n" + right);
            }
        } catch (Exception exception) {
            throw new BusinessException("重新处理失败：" + exception.getMessage());
        }
        return processText(document, text);
    }

    @Transactional
    public KnowledgeDocument updateMetadata(Long documentId, String department, String visibility) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("文档不存在"));
        if (department != null && !department.isBlank()) {
            document.setDepartment(department);
        }
        if (visibility != null && !visibility.isBlank()) {
            document.setVisibility(visibility);
        }
        document.setUpdatedAt(LocalDateTime.now());
        documentRepository.save(document);
        for (KnowledgeChunk chunk : chunkRepository.findByDocumentIdOrderByChunkNoAsc(documentId)) {
            chunk.setDepartment(document.getDepartment());
            chunk.setVisibility(document.getVisibility());
            chunkRepository.save(chunk);
        }
        return rebuild(documentId);
    }

    @Transactional
    public void delete(Long documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new BusinessException("文档不存在");
        }
        vectorStore.deleteByDocumentId(documentId);
        chunkRepository.deleteByDocumentId(documentId);
        documentRepository.deleteById(documentId);
    }

    private KnowledgeDocument processText(KnowledgeDocument document, String text) {
        try {
            document.setStatus(DocumentStatus.PARSING);
            document.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(document);
            String normalized = normalize(text);
            if (normalized.isBlank()) {
                throw new BusinessException("无法从文档中解析出有效文本");
            }
            document.setSummary(summary(normalized));
            document.setStatus(DocumentStatus.CHUNKING);
            documentRepository.save(document);
            List<String> slices = textChunker.chunk(normalized);
            chunkRepository.deleteByDocumentId(document.getId());
            vectorStore.deleteByDocumentId(document.getId());
            document.setStatus(DocumentStatus.VECTORIZING);
            documentRepository.save(document);
            List<KnowledgeChunk> savedChunks = new ArrayList<>();
            for (int i = 0; i < slices.size(); i++) {
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setDocumentId(document.getId());
                chunk.setChunkNo(i + 1);
                chunk.setPageNo(i / 3 + 1);
                chunk.setContent(slices.get(i));
                chunk.setFileName(document.getOriginalName());
                chunk.setDepartment(document.getDepartment());
                chunk.setVisibility(document.getVisibility());
                savedChunks.add(chunkRepository.save(chunk));
            }
            vectorStore.upsert(savedChunks.stream()
                    .map(chunk -> new VectorChunk(
                            String.valueOf(chunk.getId()),
                            chunk.getId(),
                            chunk.getDocumentId(),
                            chunk.getChunkNo(),
                            chunk.getPageNo(),
                            chunk.getContent(),
                            chunk.getFileName(),
                            chunk.getDepartment(),
                            chunk.getVisibility(),
                            embeddingService.embed(chunk.getContent())))
                    .toList());
            for (KnowledgeChunk chunk : savedChunks) {
                chunk.setVectorId(String.valueOf(chunk.getId()));
                chunk.setVectorState("INDEXED");
            }
            chunkRepository.saveAll(savedChunks);
            document.setChunkCount(savedChunks.size());
            document.setStatus(DocumentStatus.INDEXED);
            document.setFailureReason(null);
            document.setUpdatedAt(LocalDateTime.now());
            return documentRepository.save(document);
        } catch (RuntimeException exception) {
            document.setStatus(DocumentStatus.FAILED);
            document.setFailureReason(exception.getMessage());
            document.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(document);
            throw exception;
        }
    }

    private String parse(InputStream stream, String name) throws IOException, TikaException {
        if (name.toLowerCase(Locale.ROOT).endsWith(".txt") || name.toLowerCase(Locale.ROOT).endsWith(".md")) {
            return new String(stream.readAllBytes());
        }
        return tika.parseToString(stream);
    }

    private KnowledgeDocument newDocument(String originalName, String extension, String department, String visibility, UserAccount user) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setFileName(UUID.randomUUID() + "-" + originalName);
        document.setOriginalName(originalName);
        document.setFileType(extension);
        document.setDepartment(blankToDefault(department, "教务处"));
        document.setVisibility(blankToDefault(visibility, "PUBLIC"));
        document.setUploadUserId(user.getId());
        document.setUploadUserName(user.getDisplayName());
        document.setStatus(DocumentStatus.PENDING);
        return document;
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace('\u00a0', ' ').replaceAll("[ \\t]+", " ").trim();
    }

    private String summary(String text) {
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() <= 260 ? compact : compact.substring(0, 260) + "...";
    }

    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index >= 0 ? fileName.substring(index + 1).toLowerCase(Locale.ROOT) : "txt";
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
