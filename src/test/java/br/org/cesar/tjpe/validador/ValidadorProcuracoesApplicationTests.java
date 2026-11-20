package br.org.cesar.tjpe.validador;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("h2")
@DisplayName("Teste de Carregamento do Contexto Spring Boot")
class ValidadorProcuracoesApplicationTests {

    @Test
    void contextLoads() {
        // Verifica se todas as dependências, repositórios, serviços e controllers sobem corretamente
    }
}
