package br.org.cesar.tjpe.validador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Ponto de entrada do Sistema de Validação Inteligente de Procurações Ad Judicia do TJPE.
 * Desenvolvido como projeto acadêmico para a CESAR School.
 */
@SpringBootApplication
public class ValidadorProcuracoesApplication {

    private static final Logger log = LoggerFactory.getLogger(ValidadorProcuracoesApplication.class);

    public static void main(String[] args) {
        Environment env = SpringApplication.run(ValidadorProcuracoesApplication.class, args).getEnvironment();
        logApplicationStartup(env);
    }

    private static void logApplicationStartup(Environment env) {
        String protocol = "http";
        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "/");
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("O endereço do host local não pôde ser determinado.");
        }

        log.info("""
                \n--------------------------------------------------------------------------------
                \tAplicação: '{}' inicializada com sucesso!
                \tPainel Web do Advogado:  {}://localhost:{}{}
                \tPainel de Rede:          {}://{}:{}{}
                \tDocumentação de Testes:  {}://localhost:{}{}/api/testes/cenarios
                \tBanco de Dados:          PostgreSQL / H2
                \tCESAR School & Tribunal de Justiça de Pernambuco (TJPE)
                --------------------------------------------------------------------------------""",
                env.getProperty("spring.application.name"),
                protocol, serverPort, contextPath,
                protocol, hostAddress, serverPort, contextPath,
                protocol, serverPort, contextPath);
    }
}
