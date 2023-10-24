package com.example.carina.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.service.OpenAiService;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import org.springframework.ai.autoconfigure.openai.OpenAiProperties;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.openai.client.OpenAiClient;
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

import java.time.Duration;
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
        return new VectorStoreRetriever(vectorStore, 4, 0.75);
    }

    // vLLM API work-around
	@Bean
    @ConditionalOnProperty(
            value="spring.ai.openai.base-url",
            havingValue = "https://openai.apps.dhaka.cf-app.com/v1/")
	public OpenAiClient openAiClient(OpenAiProperties openAiProperties) {
		OpenAiClient openAiClient = new OpenAiClient(theoOpenAiService(openAiProperties.getBaseUrl(),
				openAiProperties.getApiKey(), openAiProperties.getDuration()));
		openAiClient.setTemperature(openAiProperties.getTemperature());
		openAiClient.setModel(openAiProperties.getModel());
		return openAiClient;
	}

	private OpenAiService theoOpenAiService(String baseUrl, String apiKey, Duration duration) {

		ObjectMapper mapper = OpenAiService.defaultObjectMapper();
		OkHttpClient client = OpenAiService.defaultClient(apiKey, duration);

		// Waiting for https://github.com/TheoKanning/openai-java/issues/249 to be
		// resolved.
		Retrofit retrofit = new Retrofit.Builder().baseUrl(baseUrl)
			.client(client)
			.addConverterFactory(JacksonConverterFactory.create(mapper))
			.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
			.build();

		OpenAiApi api = retrofit.create(VllmOpenAiApi.class);

		return new OpenAiService(api);
	}

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins(
                        "http://localhost:3000",
                        "https://localization.carina.org",
                        "https://www.localization.carina.org",
                        "https://dev.carina.org",
                        "https://www.dev.carina.org",
                        "https://preview.carina.org",
                        "https://www.preview.carina.org",
                        "https://carina.org",
                        "https://www.carina.org"
                );
            }
        };
    }
}
