package com.nexoia.knowledge.embedding.service;

import java.util.List;

record OllamaEmbedRequest(String model, List<String> input) {
}
