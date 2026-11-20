package br.org.cesar.tjpe.validador.dto;

import br.org.cesar.tjpe.validador.domain.enums.TipoDocumento;
import java.time.LocalDateTime;

/**
 * Resposta completa com metadados do arquivo, texto OCR bruto, dados estruturados e resultado do parecer.
 */
public record ProcuracaoDetalheResponseDTO(
        Long id,
        String nomeArquivo,
        TipoDocumento tipoDocumento,
        Long tamanhoBytes,
        String hashSha256,
        LocalDateTime dataUpload,
        String textoExtraidoOcr,
        DadosExtraidosDTO dadosExtraidos,
        ResultadoValidacaoDTO resultadoValidacao
) {}
