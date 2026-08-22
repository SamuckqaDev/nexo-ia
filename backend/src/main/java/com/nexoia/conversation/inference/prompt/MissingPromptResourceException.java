package com.nexoia.conversation.inference.prompt;

/**
 * Raised at startup when a mandatory prompt resource is absent or blank. Failing fast is deliberate:
 * a blank identity or rules file would silently strip the assistant's governance from every request.
 */
public class MissingPromptResourceException extends RuntimeException {

    public MissingPromptResourceException(PromptResource resource, String reason) {
        super("Mandatory prompt resource " + resource.path() + " " + reason);
    }
}
