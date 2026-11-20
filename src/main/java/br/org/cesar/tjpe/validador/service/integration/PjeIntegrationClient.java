package br.org.cesar.tjpe.validador.service.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Cliente de Integração com o Processo Judicial Eletrônico (PJe) do TJPE.
 * 
 * ATENÇÃO - AVISO OBRIGATÓRIO DE AMBIENTE ACADÊMICO:
 * Por se tratar de um projeto acadêmico para a CESAR School sem credenciais de barramento oficial
 * da PDPJ / TJPE em ambiente produtivo, esta classe implementa a interface oficial prevista,
 * porém opera em MODO DE DEMONSTRAÇÃO SIMULADA com dados identificados expressamente como demonstração.
 */
@Service
public class PjeIntegrationClient {

    private static final Logger log = LoggerFactory.getLogger(PjeIntegrationClient.class);

    public record ConsultaProcessoResult(
            boolean simulado,
            String avisoAcademico,
            boolean processoLocalizado,
            String poloAtivoNome,
            String poloAtivoCpfCnpj,
            String varaComarca,
            String statusProcessual
    ) {}

    public ConsultaProcessoResult consultarProcessoNoPje(String numeroCnj) {
        log.warn("[INTEGRAÇÃO SIMULADA - ACADÊMICO CESAR/TJPE] Consultando processo '{}' no barramento PJe (MODO DEMONSTRAÇÃO)", numeroCnj);

        String aviso = "DEMONSTRAÇÃO ACADÊMICA CESAR SCHOOL: Consulta simulada ao PJe TJPE (Resolução CNJ nº 335/2020 PDPJ-Br).";

        if (numeroCnj == null || numeroCnj.isBlank()) {
            return new ConsultaProcessoResult(true, aviso, false, null, null, null, "NÚMERO NÃO FORNECIDO");
        }

        // Simulação: se o processo termina com dígitos de erro ou específico de teste
        if (numeroCnj.contains("9999999")) {
            return new ConsultaProcessoResult(true, aviso, false, null, null, null, "PROCESSO INEXISTENTE NO PJe TJPE (SIMULADO)");
        }

        // Retorna simulação de processo ativo na 1ª Vara Cível da Capital - TJPE
        return new ConsultaProcessoResult(
                true,
                aviso,
                true,
                "MARIA SILVA SANTOS",
                "123.456.789-00",
                "1ª Vara Cível da Comarca de Recife - TJPE",
                "TRAMITAÇÃO REGULAR"
        );
    }
}
