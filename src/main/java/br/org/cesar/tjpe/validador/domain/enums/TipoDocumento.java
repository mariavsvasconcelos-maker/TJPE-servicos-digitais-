package br.org.cesar.tjpe.validador.domain.enums;

/**
 * Tipo de documento de entrada recebido pelo validador.
 */
public enum TipoDocumento {
    PDF,
    IMAGEM_JPG,
    IMAGEM_PNG,
    DESCONHECIDO;

    public static TipoDocumento fromContentType(String contentType, String filename) {
        if (contentType != null) {
            String lower = contentType.toLowerCase();
            if (lower.contains("pdf")) return PDF;
            if (lower.contains("jpeg") || lower.contains("jpg")) return IMAGEM_JPG;
            if (lower.contains("png")) return IMAGEM_PNG;
        }
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".pdf")) return PDF;
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return IMAGEM_JPG;
            if (lower.endsWith(".png")) return IMAGEM_PNG;
        }
        return DESCONHECIDO;
    }
}
