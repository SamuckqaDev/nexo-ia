package com.nexoia.knowledge.embedding.service;

import java.util.List;

record OllamaEmbedResponse(List<List<Float>> embeddings) {
}
