package br.org.cesar.tjpe.validador.service.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Cliente de Integração com o Cadastro Nacional dos Advogados (CNA / OAB).
 * 
 * ATENÇÃO - AVISO OBRIGATÓRIO DE AMBIENTE ACADÊMICO:
 * Conforme orientações da CESAR School e do TJPE, na ausência de webservice governamental
 * com autenticação mTLS em produção, esta classe fornece a arquitetura preparada para consumo da API
 * do CNA, operando em MODO DE DEMONSTRAÇÃO SIMULADA com alerta acadêmico explícito.
 */
@Service
public class CnaOabIntegrationClient {

    private static final Logger log = LoggerFactory.getLogger(CnaOabIntegrationClient.class);

    public record ConsultaOabResult(
            boolean simulado,
            String avisoAcademico,
            boolean regular,
            String nomeAdvogado,
            String numeroInscricao,
            String seccional,
            String situacaoCadastral,
            String tipoInscricao
    ) {}

    public ConsultaOabResult consultarInscricao(String numeroOab, String seccional) {
        log.warn("[INTEGRAÇÃO SIMULADA - ACADÊMICO CESAR/OAB] Consultando OAB {}/{} no Cadastro Nacional dos Advogados (MODO DEMONSTRAÇÃO)",
                numeroOab, seccional);

        String aviso = "DEMONSTRAÇÃO ACADÊMICA CESAR SCHOOL: Consulta simulada ao Cadastro Nacional dos Advogados (CNA/CFOAB).";

        if (numeroOab == null || numeroOab.isBlank()) {
            return new ConsultaOabResult(true, aviso, false, null, null, null, "NÃO FORNECIDO", null);
        }

        String limpoNum = numeroOab.replaceAll("\\D", "");
        String sec = (seccional != null && !seccional.isBlank()) ? seccional.trim().toUpperCase() : "PE";

        // Caso simulado de teste para advogado suspenso/cancelado
        if (limpoNum.endsWith("0000") || limpoNum.contains("99999")) {
            return new ConsultaOabResult(
                    true,
                    aviso,
                    false,
                    "ADVOGADO COM INSCRIÇÃO CANCELADA (SIMULADO)",
                    limpoNum,
                    sec,
                    "CANCELADA / SUSPENSA",
                    "ADVOGADO"
            );
        }

        // Caso regular padrão
        return new ConsultaOabResult(
                true,
                aviso,
                true,
                "DR. CARLOS EDUARDO DE ALBUQUERQUE",
                limpoNum,
                sec,
                "REGULAR - ATIVO",
                "ADVOGADO"
        );
    }
}
