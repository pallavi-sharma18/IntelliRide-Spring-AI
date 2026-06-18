package com.flourish.intelliride;

import com.flourish.intelliride.services.RAGService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RAGServiceTests {
    @Autowired
    private RAGService ragService;

    @Test
    public void testIngest() {
        ragService.ingestVectorStore();
    }
}
