package com.manas.rag.document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.manas.rag.common.ApiException;
import com.manas.rag.config.AppProperties;

/** Steps 3 to 7: take the PDF, read it page by page, chunk it, embed the chunks, save them in pgvector. */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final PdfDocumentRepository documentRepository;
    private final VectorStore vectorStore;
    private final TextChunker chunker;

    public IngestionService(PdfDocumentRepository documentRepository, VectorStore vectorStore,
            AppProperties properties) {
        this.documentRepository = documentRepository;
        this.vectorStore = vectorStore;
        this.chunker = new TextChunker(properties.rag().chunkWords(), properties.rag().chunkOverlapWords());
    }

    public PdfDocument ingest(UUID workspaceId, MultipartFile file) {
        byte[] bytes = readPdfBytes(file);
        String filename = cleanFilename(file.getOriginalFilename());

        // Step 4: one Document per page, with the page number in its metadata.
        List<Document> pages = readPages(bytes, filename);

        PdfDocument document = documentRepository.save(new PdfDocument(workspaceId, filename, bytes.length));

        // Step 5: chunk each page, remembering where every chunk came from.
        List<Document> chunks = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            Object pageMeta = pages.get(i).getMetadata().get(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER);
            int pageNumber = pageMeta instanceof Number n ? n.intValue() : i + 1;
            for (String text : chunker.chunk(pages.get(i).getText())) {
                chunks.add(new Document(text, Map.of(
                        VectorMetadata.WORKSPACE_ID, workspaceId.toString(),
                        VectorMetadata.DOCUMENT_ID, document.getId().toString(),
                        VectorMetadata.FILENAME, filename,
                        VectorMetadata.PAGE, pageNumber,
                        VectorMetadata.CHUNK_INDEX, chunks.size())));
            }
        }

        if (chunks.isEmpty()) {
            documentRepository.delete(document);
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No text found in this PDF. Scanned (image-only) PDFs are not supported.");
        }

        try {
            // Steps 6 and 7: the vector store calls the embedding model, then writes text + vector + metadata.
            vectorStore.add(chunks);
        } catch (RuntimeException ex) {
            log.error("Embedding or storing failed for {}", filename, ex);
            cleanUp(document);
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Could not create embeddings. Is Ollama running, and is the embedding model downloaded?");
        }

        document.markIngested(pages.size(), chunks.size());
        log.info("Ingested {}: {} pages, {} chunks", filename, pages.size(), chunks.size());
        return documentRepository.save(document);
    }

    public void delete(PdfDocument document) {
        vectorStore.delete(VectorMetadata.documentFilter(document.getId()));
        documentRepository.delete(document);
    }

    private void cleanUp(PdfDocument document) {
        try {
            delete(document);
        } catch (RuntimeException ex) {
            log.warn("Clean-up after a failed upload did not finish for document {}", document.getId(), ex);
        }
    }

    private static byte[] readPdfBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Attach a PDF in the form field named 'file'.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw ApiException.badRequest("Could not read the uploaded file.");
        }
        // Check the file's first bytes, not just its name or content type, which the sender controls.
        if (bytes.length < PDF_MAGIC.length || !startsWith(bytes, PDF_MAGIC)) {
            throw ApiException.badRequest("Only PDF files are supported.");
        }
        return bytes;
    }

    private static List<Document> readPages(byte[] bytes, String filename) {
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        try {
            PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder().withPagesPerDocument(1).build();
            return new PagePdfDocumentReader(resource, config).get();
        } catch (RuntimeException ex) {
            log.warn("Could not parse PDF {}", filename, ex);
            throw ApiException.badRequest("This PDF could not be read. It may be damaged or password-protected.");
        }
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    /** Keeps only the last path segment and drops control characters; the name is display-only. */
    private static String cleanFilename(String original) {
        if (original == null || original.isBlank()) {
            return "document.pdf";
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (name.isEmpty()) {
            return "document.pdf";
        }
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }
}
