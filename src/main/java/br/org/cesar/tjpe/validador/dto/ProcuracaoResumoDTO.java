package br.org.cesar.tjpe.validador.dto;

import br.org.cesar.tjpe.validador.domain.enums.StatusValidacao;
import br.org.cesar.tjpe.validador.domain.enums.TipoDocumento;
import java.time.LocalDateTime;

public record ProcuracaoResumoDTO(
        Long id,
        String nomeArquivo,
        TipoDocumento tipoDocumento,
        LocalDateTime dataUpload,
        String numeroProcessoCnj,
        String outorganteNome,
        String outorgadoNome,
        StatusValidacao status,
        int totalJustificativas
) {}
