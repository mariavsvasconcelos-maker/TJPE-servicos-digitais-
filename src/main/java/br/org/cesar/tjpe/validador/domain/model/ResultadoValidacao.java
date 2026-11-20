package br.org.cesar.tjpe.validador.domain.model;

import br.org.cesar.tjpe.validador.domain.enums.StatusValidacao;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado formal da validação das regras de negócio do TJPE.
 */
@Entity
@Table(name = "resultados_validacao")
public class ResultadoValidacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StatusValidacao status;

    @Column(nullable = false)
    private LocalDateTime dataValidacao = LocalDateTime.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "justificativas_validacao", joinColumns = @JoinColumn(name = "resultado_id"))
    @Column(name = "justificativa", length = 1000)
    private List<String> justificativas = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "detalhes_regras_validacao", joinColumns = @JoinColumn(name = "resultado_id"))
    @Column(name = "detalhe_regra", length = 1000)
    private List<String> regrasDetalhadas = new ArrayList<>();

    public ResultadoValidacao() {}

    public ResultadoValidacao(StatusValidacao status, List<String> justificativas, List<String> regrasDetalhadas) {
        this.status = status;
        this.justificativas = justificativas != null ? justificativas : new ArrayList<>();
        this.regrasDetalhadas = regrasDetalhadas != null ? regrasDetalhadas : new ArrayList<>();
        this.dataValidacao = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public StatusValidacao getStatus() {
        return status;
    }

    public void setStatus(StatusValidacao status) {
        this.status = status;
    }

    public LocalDateTime getDataValidacao() {
        return dataValidacao;
    }

    public void setDataValidacao(LocalDateTime dataValidacao) {
        this.dataValidacao = dataValidacao;
    }

    public List<String> getJustificativas() {
        return justificativas;
    }

    public void setJustificativas(List<String> justificativas) {
        this.justificativas = justificativas != null ? justificativas : new ArrayList<>();
    }

    public List<String> getRegrasDetalhadas() {
        return regrasDetalhadas;
    }

    public void setRegrasDetalhadas(List<String> regrasDetalhadas) {
        this.regrasDetalhadas = regrasDetalhadas != null ? regrasDetalhadas : new ArrayList<>();
    }
}
