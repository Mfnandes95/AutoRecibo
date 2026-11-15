package com.autorecibo.api.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * Verifica se a porta configurada (ex: 8080) está ocupada por outra aplicação
 * e, caso esteja em uso, seleciona automaticamente a próxima porta livre.
 */
@Slf4j
@Configuration
public class ServerPortConfig {

    @Value("${server.port:8080}")
    private int configuredPort;

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> portFallbackCustomizer() {
        return factory -> {
            if (configuredPort <= 0) {
                return;
            }
            if (!isPortAvailable(configuredPort)) {
                int freePort = findNextAvailablePort(configuredPort + 1, configuredPort + 100);
                log.warn("Porta {} já está ocupada por outra aplicação. Alternando automaticamente para a porta livre: {}",
                        configuredPort, freePort);
                factory.setPort(freePort);
            }
        };
    }

    public static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress("0.0.0.0", port), 1);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public static int findNextAvailablePort(int minPort, int maxPort) {
        for (int port = minPort; port <= maxPort; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException("Nenhuma porta TCP livre encontrada.", ex);
        }
    }
}
