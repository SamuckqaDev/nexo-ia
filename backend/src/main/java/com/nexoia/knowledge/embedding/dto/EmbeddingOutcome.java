package com.nexoia.knowledge.embedding.dto;

import java.util.List;

public record EmbeddingOutcome(List<float[]> embeddings, String model, int dimensions) {
}
