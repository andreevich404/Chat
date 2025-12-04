package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatServer {
    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);

    public static void main(String[] args) {
        logger.info("Chat server started");
        logger.debug("Debugging server initialization");
        logger.warn("This is a warning message");
        logger.error("An error occurred while initializing the server");
    }
}