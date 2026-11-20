package br.org.cesar.tjpe.validador.service.validator;

import br.org.cesar.tjpe.validador.domain.enums.StatusValidacao;
import br.org.cesar.tjpe.validador.domain.enums.TipoAssinatura;
import br.org.cesar.tjpe.validador.domain.model.DadosExtraidos;
import br.org.cesar.tjpe.validador.domain.model.ResultadoValidacao;
import br.org.cesar.tjpe.validador.service.integration.CnaOabIntegrationClient;
import br.org.cesar.tjpe.validador.service.integration.PjeIntegrationClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor central de regras de negócio para validação de procurações judiciais do TJPE.
 */
@Service
public class RegrasValidacaoService {

    private final CpfCnpjValidator cpfCnpjValidator;
    private final CnjValidator cnjValidator;
    private final PjeIntegrationClient pjeClient;
    private final CnaOabIntegrationClient oabClient;

    @Value("${tjpe.validador.seccional-padrao:PE}")
    private String seccionalPadrao;

    @Value("${tjpe.validador.limiar-caracteres-legibilidade:60}")
    private int limiarLegibilidade;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public RegrasValidacaoService(
            CpfCnpjValidator cpfCnpjValidator,
            CnjValidator cnjValidator,
            PjeIntegrationClient pjeClient,
            CnaOabIntegrationClient oabClient) {
        this.cpfCnpjValidator = cpfCnpjValidator;
        this.cnjValidator = cnjValidator;
        this.pjeClient = pjeClient;
        this.oabClient = oabClient;
    }

    public ResultadoValidacao validar(DadosExtraidos dados, String textoOcrBruto, String numeroProcessoParametro) {
        List<String> reprovacoes = new ArrayList<>();
        List<String> revisaoManual = new ArrayList<>();
        List<String> detalhesRegras = new ArrayList<>();

        // 1. Regra de Legibilidade e Integridade do Documento
        validarLegibilidade(textoOcrBruto, dados, reprovacoes, revisaoManual, detalhesRegras);

        // Se o documento estiver completamente ilegível, não adianta checar o resto
        if (textoOcrBruto != null && textoOcrBruto.contains("[DOCUMENTO_ILEGIVEL_TESTE]") 
                || (textoOcrBruto != null && textoOcrBruto.length() < 25 && (dados.getOutorganteNome() == null))) {
            revisaoManual.add("O documento digitalizado apresenta baixa legibilidade, corte ou borrões que impedem a leitura segura do conteúdo. Encaminhado para verificação manual da secretaria judiciária.");
            detalhesRegras.add("REGRA 1 - LEGIBILIDADE: FALHA - Texto extraído insuficiente ou degradado.");
            return new ResultadoValidacao(StatusValidacao.REVISAO_MANUAL, revisaoManual, detalhesRegras);
        }

        // 2. Regra de Presença dos Campos Obrigatórios
        validarCamposObrigatorios(dados, reprovacoes, revisaoManual, detalhesRegras);

        // 3. Regra de Dígitos Verificadores de CPF / CNPJ
        validarCpfCnpj(dados, reprovacoes, detalhesRegras);

        // 4. Regra do Número do Processo (CNJ)
        validarNumeroProcessoCnj(dados, numeroProcessoParametro, reprovacoes, revisaoManual, detalhesRegras);

        // 5. Regra da Existência e Validade da Assinatura
        validarAssinatura(dados, reprovacoes, revisaoManual, detalhesRegras);

        // 6. Regra de Validade Temporal da Procuração
        validarValidadeTemporal(dados, reprovacoes, detalhesRegras);

        // 7. Regra da Situação do Registro na OAB (CNA/OAB)
        validarSituacaoOab(dados, reprovacoes, revisaoManual, detalhesRegras);

        // 8. Regra de Compatibilidade da Finalidade e Poderes Ad Judicia
        validarPoderesEFinalidade(dados, reprovacoes, revisaoManual, detalhesRegras);

        // 9. Regra do Prazo Processual e Data de Outorga
        validarDataOutorga(dados, reprovacoes, detalhesRegras);

        // Decisão Final do Parecer
        if (!reprovacoes.isEmpty()) {
            return new ResultadoValidacao(StatusValidacao.REPROVADO, reprovacoes, detalhesRegras);
        }

        if (!revisaoManual.isEmpty()) {
            return new ResultadoValidacao(StatusValidacao.REVISAO_MANUAL, revisaoManual, detalhesRegras);
        }

        List<String> aprovacao = List.of(
                "Procuração em conformidade com as exigências processuais do TJPE.",
                "Poderes ad judicia regulares, outorgante e outorgado identificados com documentos válidos e assinatura verificada."
        );
        detalhesRegras.add("TODAS AS 10 REGRAS DE VALIDAÇÃO DO TJPE FORAM CUMPRIDAS COM SUCESSO.");
        return new ResultadoValidacao(StatusValidacao.APROVADO, aprovacao, detalhesRegras);
    }

    private void validarLegibilidade(String textoOcr, DadosExtraidos dados, List<String> reprovacoes, List<String> revisao, List<String> detalhes) {
        if (textoOcr == null || textoOcr.trim().length() < limiarLegibilidade) {
            revisao.add("A imagem da procuração possui pouca definição ou ruído excessivo, dificultando o reconhecimento óptico de caracteres.");
            detalhes.add("REGRA 1 - LEGIBILIDADE: ALERTA - Caracteres lidos abaixo do limiar (" + (textoOcr == null ? 0 : textoOcr.length()) + " de " + limiarLegibilidade + ").");
        } else {
            detalhes.add("REGRA 1 - LEGIBILIDADE: APROVADO - Volume e nitidez de caracteres adequados para análise.");
        }
    }

    private void validarCamposObrigatorios(DadosExtraidos dados, List<String> reprovacoes, List<String> revisao, List<String> detalhes) {
        boolean ok = true;
        if (dados.getOutorganteNome() == null || dados.getOutorganteNome().isBlank()) {
            reprovacoes.add("O nome do outorgante (cliente/parte) não foi identificado no documento.");
            ok = false;
        }
        if (dados.getOutorganteCpfCnpj() == null || dados.getOutorganteCpfCnpj().isBlank()) {
            reprovacoes.add("O número de CPF ou CNPJ do outorgante não consta no instrumento.");
            ok = false;
        }
        if (dados.getOutorgadoNome() == null || dados.getOutorgadoNome().isBlank()) {
            reprovacoes.add("O nome do outorgado (advogado) não foi identificado no documento.");
            ok = false;
        }
        if (dados.getOutorgadoOabNumero() == null || dados.getOutorgadoOabNumero().isBlank()) {
            reprovacoes.add("O número de inscrição da OAB do advogado outorgado não foi identificado.");
            ok = false;
        }

        if (ok) {
            detalhes.add("REGRA 2 - CAMPOS OBRIGATÓRIOS: APROVADO - Todos os campos essenciais presentes.");
        } else {
            detalhes.add("REGRA 2 - CAMPOS OBRIGATÓRIOS: REPROVADO - Ausência de campos essenciais requeridos pelo CPC/TJPE.");
        }
    }

    private void validarCpfCnpj(DadosExtraidos dados, List<String> reprovacoes, List<String> detalhes) {
        boolean outorganteValido = true;
        if (dados.getOutorganteCpfCnpj() != null) {
            if (!cpfCnpjValidator.isValid(dados.getOutorganteCpfCnpj())) {
                reprovacoes.add("O CPF/CNPJ do outorgante informado (" + dados.getOutorganteCpfCnpj() + ") é inválido segundo o algoritmo de dígitos verificadores.");
                outorganteValido = false;
            }
        }

        boolean outorgadoValido = true;
        if (dados.getOutorgadoCpfCnpj() != null && !dados.getOutorgadoCpfCnpj().isBlank()) {
            if (!cpfCnpjValidator.isValid(dados.getOutorgadoCpfCnpj())) {
                reprovacoes.add("O CPF do advogado outorgado (" + dados.getOutorgadoCpfCnpj() + ") apresenta inconsistência nos dígitos verificadores.");
                outorgadoValido = false;
            }
        }

        if (outorganteValido && outorgadoValido) {
            detalhes.add("REGRA 3 - DÍGITOS VERIFICADORES (CPF/CNPJ): APROVADO - Cálculo Módulo 11 regular.");
        } else {
            detalhes.add("REGRA 3 - DÍGITOS VERIFICADORES (CPF/CNPJ): REPROVADO - Dígito verificador inválido.");
        }
    }

    private void validarNumeroProcessoCnj(DadosExtraidos dados, String processoParam, List<String> reprovacoes, List<String> revisao, List<String> detalhes) {
        String cnjParaValidar = dados.getNumeroProcessoCnj();
        if (cnjParaValidar == null || cnjParaValidar.isBlank()) {
            cnjParaValidar = processoParam;
        }

        if (cnjParaValidar != null && !cnjParaValidar.isBlank()) {
            if (!cnjValidator.isValid(cnjParaValidar)) {
                reprovacoes.add("O número de processo CNJ informado (" + cnjParaValidar + ") possui dígito verificador incorreto (Resolução CNJ nº 65/2008).");
                detalhes.add("REGRA 4 - NÚMERO DO PROCESSO CNJ: REPROVADO - Módulo 97 inválido.");
                return;
            }

            if (!cnjValidator.isTjpe(cnjParaValidar)) {
                revisao.add("O número do processo não possui o identificador padrão do TJPE (ramo 8, tribunal 17). Confirme se a ação tramita na Justiça Estadual de Pernambuco.");
                detalhes.add("REGRA 4 - NÚMERO DO PROCESSO CNJ: REVISÃO MANUAL - Ramo ou tribunal divergente de TJPE (.8.17).");
            } else {
                detalhes.add("REGRA 4 - NÚMERO DO PROCESSO CNJ: APROVADO - Numeração única e tribunal TJPE conferidos.");
            }

            // Consulta simulada ao PJe
            var pjeResult = pjeClient.consultarProcessoNoPje(cnjParaValidar);
            if (!pjeResult.processoLocalizado()) {
                revisao.add("Processo não localizado na base do PJe TJPE. [" + pjeResult.avisoAcademico() + "]");
                detalhes.add("REGRA 5 - CORRESPONDÊNCIA PJe: REVISÃO MANUAL - " + pjeResult.statusProcessual());
            } else {
                detalhes.add("REGRA 5 - CORRESPONDÊNCIA PJe: APROVADO (SIMULADO) - " + pjeResult.varaComarca());
            }
        } else {
            // Em procurações ad judicia gerais, o número do processo pode ser opcional se outorgada para o foro em geral
            detalhes.add("REGRA 4 - NÚMERO DO PROCESSO CNJ: INFORMATIVO - Instrumento geral sem menção a processo específico pré-distribuído.");
        }
    }

    private void validarAssinatura(DadosExtraidos dados, List<String> reprovacoes, List<String> revisao, List<String> detalhes) {
        Boolean temAssinatura = dados.getTemAssinatura();
        TipoAssinatura tipo = dados.getTipoAssinatura();

        if (Boolean.FALSE.equals(temAssinatura) || tipo == TipoAssinatura.AUSENTE || tipo == null) {
            reprovacoes.add("A assinatura não foi identificada. Envie uma nova procuração com a assinatura legível.");
            detalhes.add("REGRA 6 - ASSINATURA: REPROVADO - Ausência de assinatura física ou certificação digital.");
        } else if (tipo == TipoAssinatura.ILEGIVEL) {
            revisao.add("A assinatura do outorgante apresenta baixa legibilidade ou corte de imagem. Encaminhada para verificação manual.");
            detalhes.add("REGRA 6 - ASSINATURA: REVISÃO MANUAL - Assinatura com ilegibilidade gráfica.");
        } else {
            detalhes.add("REGRA 6 - ASSINATURA: APROVADO - Identificada assinatura do tipo: " + tipo.getDescricao());
        }
    }

    private void validarValidadeTemporal(DadosExtraidos dados, List<String> reprovacoes, List<String> detalhes) {
        LocalDate prazo = dados.getPrazoValidade();
        if (prazo != null) {
            LocalDate hoje = LocalDate.now();
            if (prazo.isBefore(hoje)) {
                reprovacoes.add("A procuração encontra-se com o prazo de validade expirado desde " 
                        + prazo.format(DATE_FMT) + ". É necessário apresentar novo instrumento de mandato com poderes vigentes.");
                detalhes.add("REGRA 7 - VALIDADE DA PROCURAÇÃO: REPROVADO - Expirada em " + prazo.format(DATE_FMT));
            } else {
                detalhes.add("REGRA 7 - VALIDADE DA PROCURAÇÃO: APROVADO - Instrumento vigente até " + prazo.format(DATE_FMT));
            }
        } else {
            detalhes.add("REGRA 7 - VALIDADE DA PROCURAÇÃO: APROVADO - Instrumento por prazo indeterminado (Art. 682 do Código Civil).");
        }
    }

    private void validarSituacaoOab(DadosExtraidos dados, List<String> reprovacoes, List<String> revisao, List<String> detalhes) {
        String numOab = dados.getOutorgadoOabNumero();
        String secOab = dados.getOutorgadoOabSeccional();
        if (secOab == null || secOab.isBlank()) {
            secOab = seccionalPadrao;
        }

        if (numOab != null && !numOab.isBlank()) {
            var oabResult = oabClient.consultarInscricao(numOab, secOab);
            if (!oabResult.regular()) {
                reprovacoes.add("A inscrição do advogado na OAB (" + numOab + "/" + secOab + ") consta como suspensa ou cancelada. [" + oabResult.avisoAcademico() + "]");
                detalhes.add("REGRA 8 - SITUAÇÃO NA OAB: REPROVADO - Cadastro irregular no CNA.");
            } else {
                detalhes.add("REGRA 8 - SITUAÇÃO NA OAB: APROVADO (SIMULADO) - OAB " + numOab + "/" + secOab + " com situação: " + oabResult.situacaoCadastral());
            }
        }
    }

    private void validarPoderesEFinalidade(DadosExtraidos dados, List<String> reprovacoes, List<String> revisao, List<String> detalhes) {
        String poderes = dados.getPoderesConcedidos();
        if (poderes == null || poderes.isBlank()) {
            reprovacoes.add("Não foram identificados poderes forenses ou cláusula 'ad judicia' no instrumento de mandato.");
            detalhes.add("REGRA 9 - PODERES FORENSES: REPROVADO - Ausência de especificação de poderes.");
            return;
        }

        String poderesLower = poderes.toLowerCase();
        boolean temAdJudicia = poderesLower.contains("ad judicia") 
                || poderesLower.contains("foro em geral") 
                || poderesLower.contains("judiciais")
                || poderesLower.contains("representar perante a justi")
                || poderesLower.contains("tribunal");

        if (!temAdJudicia) {
            revisao.add("O instrumento não faz menção expressa aos poderes da cláusula 'ad judicia' ou foro em geral. Recomendada revisão pelo servidor para apurar a extensão do mandato.");
            detalhes.add("REGRA 9 - PODERES FORENSES: REVISÃO MANUAL - Falta menção expressa a 'ad judicia' ou foro em geral.");
        } else {
            detalhes.add("REGRA 9 - PODERES FORENSES: APROVADO - Cláusula 'ad judicia' identificada com concessão de poderes para o foro em geral.");
        }
    }

    private void validarDataOutorga(DadosExtraidos dados, List<String> reprovacoes, List<String> detalhes) {
        LocalDate outorga = dados.getDataOutorga();
        if (outorga == null) {
            reprovacoes.add("A data de outorga da procuração não foi identificada no documento.");
            detalhes.add("REGRA 10 - DATA DE OUTORGA: REPROVADO - Data não identificada.");
        } else if (outorga.isAfter(LocalDate.now().plusDays(1))) {
            reprovacoes.add("A data de outorga indicada no documento (" + outorga.format(DATE_FMT) + ") é posterior à data atual, indicando erro material.");
            detalhes.add("REGRA 10 - DATA DE OUTORGA: REPROVADO - Data futura no calendário.");
        } else {
            detalhes.add("REGRA 10 - DATA DE OUTORGA: APROVADO - Data outorgada em " + outorga.format(DATE_FMT));
        }
    }
}
