package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatClient {
    private static final Logger logger = LoggerFactory.getLogger(ChatClient.class);

    public static void main(String[] args) {
        logger.info("Chat client started");
        logger.debug("Debugging client initialization");
        logger.warn("This is a warning message");
        logger.error("An error occurred while initializing the client");
    }
}