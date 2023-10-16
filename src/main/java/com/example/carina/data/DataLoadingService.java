package com.example.carina.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.pdf.layout.PageExtractedTextFormatter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class DataLoadingService {
    private static final Logger logger = LoggerFactory.getLogger(DataLoadingService.class);

    @Value("classpath:/data/medicaid-wa-faqs.pdf")
    private Resource pdfResource;

    private final VectorStore vectorStore;

    @Autowired
    public DataLoadingService(VectorStore vectorStore) {
        Assert.notNull(vectorStore, "VectorStore must not be null.");
        this.vectorStore = vectorStore;
    }

    public void load() {
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                this.pdfResource,
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(PageExtractedTextFormatter.builder()
                                .withNumberOfBottomTextLinesToDelete(3)
                                .withNumberOfTopPagesToSkipBeforeDelete(1)
                                // .withLeftAlignment(true)
                                .build())
                        .withPagesPerDocument(1)
                        .build());

        var textSplitter = new TokenTextSplitter();

        logger.info("Parsing document, splitting, creating embeddings and storing in vector store...  this will take a while.");
        this.vectorStore.accept(
                textSplitter.apply(
                        pdfReader.get()));
        logger.info("Done parsing document, splitting, creating embeddings and storing in vector store");

    }
}
