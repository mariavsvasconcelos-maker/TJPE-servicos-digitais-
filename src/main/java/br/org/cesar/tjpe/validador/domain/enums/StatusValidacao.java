package br.org.cesar.tjpe.validador.domain.enums;

/**
 * Status final da validação da procuração de acordo com os critérios do TJPE.
 */
public enum StatusValidacao {
    APROVADO("Aprovado", "Todos os critérios obrigatórios foram atendidos com sucesso."),
    REPROVADO("Reprovado", "Foram identificadas inconsistências, irregularidades ou ausência de campos obrigatórios."),
    REVISAO_MANUAL("Revisão Manual", "Existem dúvidas ou inconsistências pontuais que demandam a análise de um servidor da vara.");

    private final String descricao;
    private final String detalhe;

    StatusValidacao(String descricao, String detalhe) {
        this.descricao = descricao;
        this.detalhe = detalhe;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getDetalhe() {
        return detalhe;
    }
}
