package br.org.cesar.tjpe.validador.dto;

import br.org.cesar.tjpe.validador.domain.enums.TipoAssinatura;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para transporte das informações extraídas da procuração.
 */
public record DadosExtraidosDTO(
        String numeroProcessoCnj,
        String outorganteNome,
        String outorganteCpfCnpj,
        String outorgadoNome,
        String outorgadoCpfCnpj,
        String outorgadoOabNumero,
        String outorgadoOabSeccional,
        LocalDate dataOutorga,
        LocalDate prazoValidade,
        String poderesConcedidos,
        String finalidade,
        Boolean temAssinatura,
        TipoAssinatura tipoAssinatura,
        List<String> carimbosSelosPresentes
) {}
