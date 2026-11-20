package br.org.cesar.tjpe.validador.dto;

import br.org.cesar.tjpe.validador.domain.enums.StatusValidacao;

public record CenarioTesteInfoDTO(
        String id,
        String titulo,
        String descricao,
        StatusValidacao statusEsperado,
        String motivoEsperado
) {}
