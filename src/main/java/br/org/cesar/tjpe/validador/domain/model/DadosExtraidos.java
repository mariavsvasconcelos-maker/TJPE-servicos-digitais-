package br.org.cesar.tjpe.validador.domain.model;

import br.org.cesar.tjpe.validador.domain.enums.TipoAssinatura;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Informações estruturadas extraídas da procuração via OCR + LLM.
 * Caso alguma informação não seja identificada, seu valor permanece null.
 */
@Embeddable
public class DadosExtraidos {

    @Column(name = "numero_processo_cnj", length = 32)
    private String numeroProcessoCnj;

    @Column(name = "outorgante_nome", length = 255)
    private String outorganteNome;

    @Column(name = "outorgante_cpf_cnpj", length = 32)
    private String outorganteCpfCnpj;

    @Column(name = "outorgado_nome", length = 255)
    private String outorgadoNome;

    @Column(name = "outorgado_cpf_cnpj", length = 32)
    private String outorgadoCpfCnpj;

    @Column(name = "outorgado_oab_numero", length = 32)
    private String outorgadoOabNumero;

    @Column(name = "outorgado_oab_seccional", length = 4)
    private String outorgadoOabSeccional;

    @Column(name = "data_outorga")
    private LocalDate dataOutorga;

    @Column(name = "prazo_validade")
    private LocalDate prazoValidade;

    @Column(name = "poderes_concedidos", length = 4000)
    private String poderesConcedidos;

    @Column(name = "finalidade", length = 2000)
    private String finalidade;

    @Column(name = "tem_assinatura")
    private Boolean temAssinatura;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_assinatura", length = 32)
    private TipoAssinatura tipoAssinatura;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> carimbosSelosPresentes = new ArrayList<>();

    public DadosExtraidos() {}

    // Getters and Setters
    public String getNumeroProcessoCnj() {
        return numeroProcessoCnj;
    }

    public void setNumeroProcessoCnj(String numeroProcessoCnj) {
        this.numeroProcessoCnj = numeroProcessoCnj;
    }

    public String getOutorganteNome() {
        return outorganteNome;
    }

    public void setOutorganteNome(String outorganteNome) {
        this.outorganteNome = outorganteNome;
    }

    public String getOutorganteCpfCnpj() {
        return outorganteCpfCnpj;
    }

    public void setOutorganteCpfCnpj(String outorganteCpfCnpj) {
        this.outorganteCpfCnpj = outorganteCpfCnpj;
    }

    public String getOutorgadoNome() {
        return outorgadoNome;
    }

    public void setOutorgadoNome(String outorgadoNome) {
        this.outorgadoNome = outorgadoNome;
    }

    public String getOutorgadoCpfCnpj() {
        return outorgadoCpfCnpj;
    }

    public void setOutorgadoCpfCnpj(String outorgadoCpfCnpj) {
        this.outorgadoCpfCnpj = outorgadoCpfCnpj;
    }

    public String getOutorgadoOabNumero() {
        return outorgadoOabNumero;
    }

    public void setOutorgadoOabNumero(String outorgadoOabNumero) {
        this.outorgadoOabNumero = outorgadoOabNumero;
    }

    public String getOutorgadoOabSeccional() {
        return outorgadoOabSeccional;
    }

    public void setOutorgadoOabSeccional(String outorgadoOabSeccional) {
        this.outorgadoOabSeccional = outorgadoOabSeccional;
    }

    public LocalDate getDataOutorga() {
        return dataOutorga;
    }

    public void setDataOutorga(LocalDate dataOutorga) {
        this.dataOutorga = dataOutorga;
    }

    public LocalDate getPrazoValidade() {
        return prazoValidade;
    }

    public void setPrazoValidade(LocalDate prazoValidade) {
        this.prazoValidade = prazoValidade;
    }

    public String getPoderesConcedidos() {
        return poderesConcedidos;
    }

    public void setPoderesConcedidos(String poderesConcedidos) {
        this.poderesConcedidos = poderesConcedidos;
    }

    public String getFinalidade() {
        return finalidade;
    }

    public void setFinalidade(String finalidade) {
        this.finalidade = finalidade;
    }

    public Boolean getTemAssinatura() {
        return temAssinatura;
    }

    public void setTemAssinatura(Boolean temAssinatura) {
        this.temAssinatura = temAssinatura;
    }

    public TipoAssinatura getTipoAssinatura() {
        return tipoAssinatura;
    }

    public void setTipoAssinatura(TipoAssinatura tipoAssinatura) {
        this.tipoAssinatura = tipoAssinatura;
    }

    public List<String> getCarimbosSelosPresentes() {
        return carimbosSelosPresentes;
    }

    public void setCarimbosSelosPresentes(List<String> carimbosSelosPresentes) {
        this.carimbosSelosPresentes = carimbosSelosPresentes != null ? carimbosSelosPresentes : new ArrayList<>();
    }
}
