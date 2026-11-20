package br.org.cesar.tjpe.validador.dto;

import br.org.cesar.tjpe.validador.domain.enums.StatusValidacao;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para o resultado das regras de validação.
 */
public record ResultadoValidacaoDTO(
        StatusValidacao status,
        String statusDescricao,
        List<String> justificativas,
        List<String> regrasDetalhadas,
        LocalDateTime dataValidacao
) {}
