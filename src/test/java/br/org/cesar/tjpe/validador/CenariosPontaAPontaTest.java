package br.org.cesar.tjpe.validador;

import br.org.cesar.tjpe.validador.controller.CenariosTesteController;
import br.org.cesar.tjpe.validador.domain.enums.StatusValidacao;
import br.org.cesar.tjpe.validador.dto.ProcuracaoDetalheResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executa os 3 cenários obrigatórios pelo pipeline completo
 * (texto do documento → OCR → IA → regras → banco), exatamente como os botões da tela.
 */
@SpringBootTest
@ActiveProfiles("h2")
@DisplayName("Cenários de ponta a ponta (do documento ao parecer)")
class CenariosPontaAPontaTest {

    @Autowired
    private CenariosTesteController cenariosController;

    private ProcuracaoDetalheResponseDTO executar(String cenario) throws Exception {
        ProcuracaoDetalheResponseDTO resposta = cenariosController.executarCenario(cenario).getBody();
        assertNotNull(resposta);
        assertNotNull(resposta.resultadoValidacao());
        return resposta;
    }

    @Test
    @DisplayName("Cenário 1: procuração válida é APROVADA, com os dados extraídos corretamente")
    void cenario1ProcuracaoValidaEhAprovada() throws Exception {
        var resposta = executar("cenario1");
        var dados = resposta.dadosExtraidos();

        assertEquals(StatusValidacao.APROVADO, resposta.resultadoValidacao().status(),
                () -> "Justificativas: " + resposta.resultadoValidacao().justificativas());
        assertEquals("529.982.247-25", dados.outorganteCpfCnpj());
        assertEquals("987.654.321-00", dados.outorgadoCpfCnpj());
        assertEquals("0001234-29.2024.8.17.0001", dados.numeroProcessoCnj());
        assertEquals(LocalDate.of(2024, 1, 10), dados.dataOutorga());
        assertNull(dados.prazoValidade(), "Prazo indeterminado não pode virar data de vencimento");
    }

    @Test
    @DisplayName("Cenário 2: procuração vencida é REPROVADA pelo prazo expirado")
    void cenario2ProcuracaoVencidaEhReprovadaPeloPrazo() throws Exception {
        var resposta = executar("cenario2");
        var resultado = resposta.resultadoValidacao();

        assertEquals(StatusValidacao.REPROVADO, resultado.status());
        assertEquals(1, resultado.justificativas().size(),
                () -> "Esperado só o motivo do prazo, veio: " + resultado.justificativas());
        assertTrue(resultado.justificativas().get(0).contains("expirado desde 15/03/2023"));
        assertEquals(LocalDate.of(2022, 3, 15), resposta.dadosExtraidos().dataOutorga());
        assertEquals(LocalDate.of(2023, 3, 15), resposta.dadosExtraidos().prazoValidade());
    }

    @Test
    @DisplayName("Cenário 3: documento ilegível vai para REVISÃO MANUAL")
    void cenario3DocumentoIlegivelVaiParaRevisaoManual() throws Exception {
        var resposta = executar("cenario3");

        assertEquals(StatusValidacao.REVISAO_MANUAL, resposta.resultadoValidacao().status());
        assertTrue(resposta.resultadoValidacao().justificativas().get(0).contains("baixa legibilidade"));
    }
}
