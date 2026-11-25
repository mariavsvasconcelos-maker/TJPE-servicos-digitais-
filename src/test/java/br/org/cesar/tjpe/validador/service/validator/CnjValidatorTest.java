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
    @DisplayName("Deve aceitar números de processo reais (ISO 7064 Mód. 97-10: DV = 98 - resto)")
    void deveValidarNumerosReaisDoPadraoCnj() {
        // Exemplo publicado na documentação da API pública DataJud, do próprio CNJ
        assertTrue(validator.isValid("0000832-35.2018.4.01.3202"));
        // Número usado no Cenário 1 de demonstração
        assertTrue(validator.isValid("0001234-29.2024.8.17.0001"));
        assertTrue(validator.isTjpe("0001234-29.2024.8.17.0001"));
        // A fórmula antiga (97 - resto) gerava DV 28, que não é válido
        assertFalse(validator.isValid("0001234-28.2024.8.17.0001"));
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
