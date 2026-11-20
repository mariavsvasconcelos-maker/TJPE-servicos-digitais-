package br.org.cesar.tjpe.validador.domain.enums;

/**
 * Identificação do tipo de assinatura encontrada no documento.
 */
public enum TipoAssinatura {
    DIGITAL("Assinatura Digital (Certificado ICP-Brasil / Gov.br / Assinador Digital)"),
    FISICA("Assinatura Física Manuscrita"),
    AUSENTE("Nenhuma assinatura identificada"),
    ILEGIVEL("Indício de assinatura ilegível ou corrompida");

    private final String descricao;

    TipoAssinatura(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
