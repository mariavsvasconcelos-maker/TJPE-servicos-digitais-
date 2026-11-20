package br.org.cesar.tjpe.validador.domain.model;

import br.org.cesar.tjpe.validador.domain.enums.TipoDocumento;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entidade central representando a procuração recebida, texto lido, metadados
 * e resultado da validação inteligente.
 */
@Entity
@Table(name = "procuracoes_documentos")
public class ProcuracaoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nomeArquivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false)
    private Long tamanhoBytes;

    @Column(length = 64)
    private String hashSha256;

    @Column(nullable = false)
    private LocalDateTime dataUpload = LocalDateTime.now();

    @Lob
    @Column(columnDefinition = "TEXT")
    private String textoExtraidoOcr;

    @Embedded
    private DadosExtraidos dadosExtraidos;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "resultado_validacao_id")
    private ResultadoValidacao resultadoValidacao;

    public ProcuracaoDocumento() {}

    public ProcuracaoDocumento(String nomeArquivo, TipoDocumento tipoDocumento, Long tamanhoBytes, String hashSha256) {
        this.nomeArquivo = nomeArquivo;
        this.tipoDocumento = tipoDocumento;
        this.tamanhoBytes = tamanhoBytes;
        this.hashSha256 = hashSha256;
        this.dataUpload = LocalDateTime.now();
        this.dadosExtraidos = new DadosExtraidos();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public Long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public void setTamanhoBytes(Long tamanhoBytes) {
        this.tamanhoBytes = tamanhoBytes;
    }

    public String getHashSha256() {
        return hashSha256;
    }

    public void setHashSha256(String hashSha256) {
        this.hashSha256 = hashSha256;
    }

    public LocalDateTime getDataUpload() {
        return dataUpload;
    }

    public void setDataUpload(LocalDateTime dataUpload) {
        this.dataUpload = dataUpload;
    }

    public String getTextoExtraidoOcr() {
        return textoExtraidoOcr;
    }

    public void setTextoExtraidoOcr(String textoExtraidoOcr) {
        this.textoExtraidoOcr = textoExtraidoOcr;
    }

    public DadosExtraidos getDadosExtraidos() {
        return dadosExtraidos;
    }

    public void setDadosExtraidos(DadosExtraidos dadosExtraidos) {
        this.dadosExtraidos = dadosExtraidos;
    }

    public ResultadoValidacao getResultadoValidacao() {
        return resultadoValidacao;
    }

    public void setResultadoValidacao(ResultadoValidacao resultadoValidacao) {
        this.resultadoValidacao = resultadoValidacao;
    }
}
