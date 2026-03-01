package md.faf223.airecommendationsservice.services;

import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class EmbeddingService {

    private ZooModel<String, float[]> model;
    private Predictor<String, float[]> predictor;

    private final Object predictorLock = new Object();

    @PostConstruct
    public void init() throws Exception {
        System.out.println("Loading Model...");
        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/intfloat/multilingual-e5-large")
                .optEngine("PyTorch")
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .optArgument("normalize", "true")
                .optArgument("truncation", "true")
                .optArgument("maxLength", "512")
                .build();

        this.model = criteria.loadModel();
        this.predictor = model.newPredictor();
    }

    public float[] encode(String text) throws Exception {
        synchronized (predictorLock) {
            return predictor.predict(text);
        }
    }

    @PreDestroy
    public void close() {
        if (predictor != null) predictor.close();
        if (model != null) model.close();
    }

}