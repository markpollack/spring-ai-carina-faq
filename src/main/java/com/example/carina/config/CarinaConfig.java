package com.example.carina.config;

import com.theokanning.openai.OpenAiHttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.retriever.VectorStoreRetriever;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.BackOffContext;
import org.springframework.retry.support.Args;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableRetry
public class CarinaConfig {

    private static final Logger logger = LoggerFactory.getLogger(CarinaConfig.class);


    // This helps with logging on retries, needs to be tidied up and moved out of the sample app.

    @Bean
    public RetryListener retryListener() {
        return new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(RetryContext context,
                                                         RetryCallback<T, E> callback,
                                                         Throwable throwable) {

                var name = context.getAttribute(RetryContext.NAME);
                var args = context.getAttribute("ARGS");
                BackOffContext backOffContext = (BackOffContext) context.getAttribute("backOffContext");

                if (throwable instanceof OpenAiHttpException oahex) {
                    if ("context_length_exceeded".equals(oahex.code)) {
                        logger.error(throwable.getMessage() +
                                "\n - abort retries!" +
                                "\n - name: " + name +
                                "\n - arguments: " + toString(args));
                        context.setExhaustedOnly();
                        return;
                    }
                }
                logger.warn(throwable.getMessage() +
                        "\n - name: " + name +
                        "\n - arguments: " + toString(args) +
                        "\n - Retry#: " + context.getRetryCount());
            }

            private String toString(Object args) {
                if (args == null)
                    return "";
                if (args instanceof Args) {
                    return Stream.of(((Args) args).getArgs())
                            .map(a -> a.toString())
                            .collect(Collectors.joining(", "));
                }
                return args.toString();
            }
        };
    }

    @Bean
    public VectorStore vectorStore(EmbeddingClient embeddingClient, JdbcTemplate jdbcTemplate) {
        return new PgVectorStore(jdbcTemplate, embeddingClient);
    }

    @Bean
    public VectorStoreRetriever vectorStoreRetriever(VectorStore vectorStore) {
        return new VectorStoreRetriever(vectorStore, 5, 0.75);
    }


}
