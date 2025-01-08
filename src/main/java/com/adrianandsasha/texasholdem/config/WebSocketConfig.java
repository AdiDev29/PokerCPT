package com.adrianandsasha.texasholdem.config;
//Importing spring. weird name for package but wtv. I will be relying on spring for all communication
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // /topic is used for broadcasting messages to multiple clients
        // /queue could be used for direct messages (not required here honestly)
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Clients will connect their potato PCs to the /ws endpoint
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}
