package com.flourish.intelliride.services;


import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RAGService {

    private final VectorStore vectorStore;

    @Value("classpath:/knowledge/*.md")
    private Resource[] knowledgeFiles;

    public void ingestVectorStore() {
        var splitter = TokenTextSplitter.builder().build();
        for (Resource file : knowledgeFiles) {
            var reader = new TextReader(file);           // tags each doc with its source
            reader.getCustomMetadata().put("source", file.getFilename() != null ? file.getFilename() : file.getDescription());
            List<Document> chunks = splitter.apply(reader.read());
            vectorStore.add(chunks);
        }
    }
}


