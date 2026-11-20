package br.org.cesar.tjpe.validador.service.validator;

import br.org.cesar.tjpe.validador.domain.enums.StatusValidacao;
import br.org.cesar.tjpe.validador.domain.enums.TipoAssinatura;
import br.org.cesar.tjpe.validador.domain.model.DadosExtraidos;
import br.org.cesar.tjpe.validador.domain.model.ResultadoValidacao;
import br.org.cesar.tjpe.validador.service.integration.CnaOabIntegrationClient;
import br.org.cesar.tjpe.validador.service.integration.PjeIntegrationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes dos 3 Cenários Obrigatórios de Validação do TJPE / CESAR School")
class RegrasValidacaoServiceTest {

    private RegrasValidacaoService regrasService;
    private CpfCnpjValidator cpfValidator;
    private CnjValidator cnjValidator;
    private PjeIntegrationClient pjeClient;
    private CnaOabIntegrationClient oabClient;

    @BeforeEach
    void setUp() {
        cpfValidator = new CpfCnpjValidator();
        cnjValidator = new CnjValidator();
        pjeClient = new PjeIntegrationClient();
        oabClient = new CnaOabIntegrationClient();
        regrasService = new RegrasValidacaoService(cpfValidator, cnjValidator, pjeClient, oabClient);
    }

    @Test
    @DisplayName("Cenário 1 Obrigatório: Deve APROVAR procuração com todos os critérios regulares")
    void deveAprovarProcuracaoValida() {
        // Dados de uma procuração regular
        DadosExtraidos dados = new DadosExtraidos();
        dados.setNumeroProcessoCnj(null); // Procuração geral ad judicia
        dados.setOutorganteNome("MARIA SILVA SANTOS");
        dados.setOutorganteCpfCnpj("529.982.247-25"); // CPF válido
        dados.setOutorgadoNome("DR. CARLOS EDUARDO DE ALBUQUERQUE");
        dados.setOutorgadoOabNumero("45678");
        dados.setOutorgadoOabSeccional("PE");
        dados.setDataOutorga(LocalDate.now().minusDays(5));
        dados.setPrazoValidade(null); // Prazo indeterminado
        dados.setPoderesConcedidos("Poderes ad judicia para o foro em geral perante o TJPE.");
        dados.setFinalidade("Representação processual judicial.");
        dados.setTemAssinatura(true);
        dados.setTipoAssinatura(TipoAssinatura.DIGITAL);

        String textoOcr = "PROCURACAO AD JUDICIA OUTORGANTE MARIA SILVA SANTOS CPF 529.982.247-25 ADVOGADO CARLOS EDUARDO OAB PE 45678 ASSINATURA DIGITAL";

        ResultadoValidacao resultado = regrasService.validar(dados, textoOcr, null);

        assertEquals(StatusValidacao.APROVADO, resultado.getStatus(), "O Cenário 1 deve ser APROVADO");
        assertFalse(resultado.getJustificativas().isEmpty());
    }

    @Test
    @DisplayName("Cenário 2 Obrigatório: Deve REPROVAR procuração com prazo de validade vencido")
    void deveReprovarProcuracaoVencida() {
        DadosExtraidos dados = new DadosExtraidos();
        dados.setOutorganteNome("JOAO PEREIRA LIMA");
        dados.setOutorganteCpfCnpj("529.982.247-25");
        dados.setOutorgadoNome("DR. LUCAS MARTINS BEZERRA");
        dados.setOutorgadoOabNumero("32100");
        dados.setOutorgadoOabSeccional("PE");
        dados.setDataOutorga(LocalDate.of(2022, 3, 15));
        // Validade expirada no passado
        dados.setPrazoValidade(LocalDate.of(2023, 3, 15));
        dados.setPoderesConcedidos("Poderes ad judicia para o foro em geral perante o TJPE.");
        dados.setFinalidade("Atuação contenciosa.");
        dados.setTemAssinatura(true);
        dados.setTipoAssinatura(TipoAssinatura.FISICA);

        String textoOcr = "PROCURACAO AD JUDICIA OUTORGANTE JOAO PEREIRA LIMA CPF 529.982.247-25 OAB PE 32100 VALIDADE 15/03/2023";

        ResultadoValidacao resultado = regrasService.validar(dados, textoOcr, null);

        assertEquals(StatusValidacao.REPROVADO, resultado.getStatus(), "O Cenário 2 deve ser REPROVADO");
        assertTrue(resultado.getJustificativas().stream().anyMatch(j -> j.contains("expirado")),
                "Deve conter justificativa explícita sobre a procuração expirada");
    }

    @Test
    @DisplayName("Cenário 3 Obrigatório: Deve encaminhar para REVISÃO MANUAL ou REPROVADO documento ilegível")
    void deveEncaminharParaRevisaoManualDocumentoIlegivel() {
        DadosExtraidos dados = new DadosExtraidos();
        // Não foi possível extrair nada com certeza (null em tudo)
        dados.setTemAssinatura(false);
        dados.setTipoAssinatura(TipoAssinatura.ILEGIVEL);

        String textoOcr = "[DOCUMENTO_ILEGIVEL_TESTE] ~*#.. %%% ???? [IMAGEM DEGRADADA - RUÍDOS EXCESSIVOS]";

        ResultadoValidacao resultado = regrasService.validar(dados, textoOcr, null);

        assertEquals(StatusValidacao.REVISAO_MANUAL, resultado.getStatus(), "O Cenário 3 deve ser REVISÃO MANUAL");
        assertTrue(resultado.getJustificativas().stream().anyMatch(j -> j.toLowerCase().contains("legibilidade")),
                "Deve conter justificativa sobre a baixa legibilidade ou ruídos");
    }
}
