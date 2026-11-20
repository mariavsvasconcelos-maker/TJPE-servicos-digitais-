package br.org.cesar.tjpe.validador.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - Validador de Numeração Única CNJ (Resolução 65/2008)")
class CnjValidatorTest {

    private CnjValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CnjValidator();
    }

    @Test
    @DisplayName("Deve calcular e validar processo com dígito verificador CNJ correto")
    void deveValidarProcessoCnjCorreto() {
        // Sequencial: 0001234, Ano: 2024, Justiça: 8, Tribunal: 17 (TJPE), Origem: 0001
        String dvEsperado = validator.calcularDigitoVerificador("0001234", "2024", "8", "17", "0001");
        String cnjCompleto = "0001234-" + dvEsperado + ".2024.8.17.0001";

        assertTrue(validator.isValid(cnjCompleto));
        assertTrue(validator.isTjpe(cnjCompleto));
    }

    @Test
    @DisplayName("Deve rejeitar CNJ com dígito verificador adulterado")
    void deveRejeitarCnjComDvInvalido() {
        String cnjAdulterado = "0001234-99.2024.8.17.0001";
        assertFalse(validator.isValid(cnjAdulterado));
    }

    @Test
    @DisplayName("Deve identificar se processo não pertence ao TJPE (.8.17)")
    void deveIdentificarOutroTribunal() {
        // Tribunal de Justiça de São Paulo: 8.26
        String cnjTjsp = "0001234-56.2024.8.26.0001";
        assertFalse(validator.isTjpe(cnjTjsp));
    }
}
