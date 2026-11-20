package br.org.cesar.tjpe.validador.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - Validador de CPF e CNPJ")
class CpfCnpjValidatorTest {

    private CpfCnpjValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CpfCnpjValidator();
    }

    @Test
    @DisplayName("Deve validar CPF válido com máscara ou sem máscara")
    void deveValidarCpfValido() {
        // CPF válido com DVs corretos
        assertTrue(validator.isCpfValid("123.456.789-09") || validator.isCpfValid("529.982.247-25"));
        assertTrue(validator.isValid("52998224725"));
    }

    @Test
    @DisplayName("Deve rejeitar CPF com dígitos verificadores incorretos")
    void deveRejeitarCpfInvalido() {
        assertFalse(validator.isCpfValid("123.456.789-99"));
        assertFalse(validator.isCpfValid("111.111.111-11")); // Sequência repetida
        assertFalse(validator.isCpfValid("12345"));
    }

    @Test
    @DisplayName("Deve validar CNPJ válido")
    void deveValidarCnpjValido() {
        // CNPJ oficial de teste válido
        assertTrue(validator.isCnpjValid("11.222.333/0001-81") || validator.isCnpjValid("00.000.000/0001-91"));
    }

    @Test
    @DisplayName("Deve rejeitar CNPJ inválido")
    void deveRejeitarCnpjInvalido() {
        assertFalse(validator.isCnpjValid("11.222.333/0001-00"));
        assertFalse(validator.isCnpjValid("00000000000000"));
    }
}
