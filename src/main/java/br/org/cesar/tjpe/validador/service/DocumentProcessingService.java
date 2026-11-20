package br.org.cesar.tjpe.validador.service;

import br.org.cesar.tjpe.validador.domain.enums.TipoDocumento;
import br.org.cesar.tjpe.validador.domain.model.DadosExtraidos;
import br.org.cesar.tjpe.validador.domain.model.ProcuracaoDocumento;
import br.org.cesar.tjpe.validador.domain.model.ResultadoValidacao;
import br.org.cesar.tjpe.validador.dto.DadosExtraidosDTO;
import br.org.cesar.tjpe.validador.dto.ProcuracaoDetalheResponseDTO;
import br.org.cesar.tjpe.validador.dto.ProcuracaoResumoDTO;
import br.org.cesar.tjpe.validador.dto.ResultadoValidacaoDTO;
import br.org.cesar.tjpe.validador.repository.ProcuracaoDocumentoRepository;
import br.org.cesar.tjpe.validador.service.image.ImagePreprocessingService;
import br.org.cesar.tjpe.validador.service.llm.LlmExtractionService;
import br.org.cesar.tjpe.validador.service.ocr.OcrService;
import br.org.cesar.tjpe.validador.service.validator.RegrasValidacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Orquestrador do Fluxo Completo de Processamento Inteligente de Procurações:
 * Upload -> OpenCV Tratamento -> Tesseract OCR -> IA / LLM -> Regras TJPE -> PostgreSQL
 */
@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final ImagePreprocessingService imagePreprocessingService;
    private final OcrService ocrService;
    private final LlmExtractionService llmExtractionService;
    private final RegrasValidacaoService regrasValidacaoService;
    private final ProcuracaoDocumentoRepository repository;

    public DocumentProcessingService(
            ImagePreprocessingService imagePreprocessingService,
            OcrService ocrService,
            LlmExtractionService llmExtractionService,
            RegrasValidacaoService regrasValidacaoService,
            ProcuracaoDocumentoRepository repository) {
        this.imagePreprocessingService = imagePreprocessingService;
        this.ocrService = ocrService;
        this.llmExtractionService = llmExtractionService;
        this.regrasValidacaoService = regrasValidacaoService;
        this.repository = repository;
    }

    @Transactional
    public ProcuracaoDetalheResponseDTO processarDocumento(MultipartFile file, String numeroProcessoParametro) throws IOException {
        String nomeOriginal = file.getOriginalFilename() != null ? file.getOriginalFilename() : "documento_procuracao";
        byte[] bytesOriginais = file.getBytes();
        long tamanho = file.getSize();
        String contentType = file.getContentType();
        TipoDocumento tipoDoc = TipoDocumento.fromContentType(contentType, nomeOriginal);
        String hashSha256 = calcularSha256(bytesOriginais);

        log.info("[PIPELINE TJPE] Iniciando processamento do arquivo '{}' ({} bytes, Tipo: {})", nomeOriginal, tamanho, tipoDoc);

        // ETAPA 1: OpenCV - Pré-processamento e Tratamento de Imagem
        var imageResult = imagePreprocessingService.processarImagem(bytesOriginais, nomeOriginal);
        byte[] bytesTratados = imageResult.processedBytes();

        // ETAPA 2: Tesseract OCR - Extração Óptica de Texto
        var ocrResult = ocrService.extrairTexto(bytesTratados, nomeOriginal);
        String textoExtraido = ocrResult.textoExtraido();

        // ETAPA 3: IA / LLM - Interpretação e Extração Estruturada JSON
        DadosExtraidos dadosExtraidos = llmExtractionService.extrairInformacoes(textoExtraido);

        // ETAPA 4: Backend Java - Aplicação das Regras de Negócio e Parecer TJPE
        ResultadoValidacao resultadoValidacao = regrasValidacaoService.validar(dadosExtraidos, textoExtraido, numeroProcessoParametro);

        // ETAPA 5: Persistência no PostgreSQL
        ProcuracaoDocumento documento = new ProcuracaoDocumento(nomeOriginal, tipoDoc, tamanho, hashSha256);
        documento.setTextoExtraidoOcr(textoExtraido);
        documento.setDadosExtraidos(dadosExtraidos);
        documento.setResultadoValidacao(resultadoValidacao);

        ProcuracaoDocumento salvo = repository.save(documento);
        log.info("[PIPELINE TJPE] Documento ID {} salvo com sucesso no banco de dados. Status do parecer: {}",
                salvo.getId(), resultadoValidacao.getStatus());

        return toDetalheDTO(salvo);
    }

    @Transactional(readOnly = true)
    public ProcuracaoDetalheResponseDTO buscarPorId(Long id) {
        ProcuracaoDocumento doc = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Procuração com ID " + id + " não encontrada no banco de dados."));
        return toDetalheDTO(doc);
    }

    @Transactional(readOnly = true)
    public List<ProcuracaoResumoDTO> listarHistorico() {
        return repository.findAllByOrderByDataUploadDesc().stream()
                .map(this::toResumoDTO)
                .toList();
    }

    private String calcularSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return "hash_unavailable";
        }
    }

    public ProcuracaoDetalheResponseDTO toDetalheDTO(ProcuracaoDocumento doc) {
        DadosExtraidos d = doc.getDadosExtraidos() != null ? doc.getDadosExtraidos() : new DadosExtraidos();
        ResultadoValidacao r = doc.getResultadoValidacao();

        DadosExtraidosDTO dadosDTO = new DadosExtraidosDTO(
                d.getNumeroProcessoCnj(),
                d.getOutorganteNome(),
                d.getOutorganteCpfCnpj(),
                d.getOutorgadoNome(),
                d.getOutorgadoCpfCnpj(),
                d.getOutorgadoOabNumero(),
                d.getOutorgadoOabSeccional(),
                d.getDataOutorga(),
                d.getPrazoValidade(),
                d.getPoderesConcedidos(),
                d.getFinalidade(),
                d.getTemAssinatura(),
                d.getTipoAssinatura(),
                d.getCarimbosSelosPresentes()
        );

        ResultadoValidacaoDTO resultadoDTO = null;
        if (r != null) {
            resultadoDTO = new ResultadoValidacaoDTO(
                    r.getStatus(),
                    r.getStatus().getDescricao(),
                    r.getJustificativas(),
                    r.getRegrasDetalhadas(),
                    r.getDataValidacao()
            );
        }

        return new ProcuracaoDetalheResponseDTO(
                doc.getId(),
                doc.getNomeArquivo(),
                doc.getTipoDocumento(),
                doc.getTamanhoBytes(),
                doc.getHashSha256(),
                doc.getDataUpload(),
                doc.getTextoExtraidoOcr(),
                dadosDTO,
                resultadoDTO
        );
    }

    private ProcuracaoResumoDTO toResumoDTO(ProcuracaoDocumento doc) {
        DadosExtraidos d = doc.getDadosExtraidos() != null ? doc.getDadosExtraidos() : new DadosExtraidos();
        ResultadoValidacao r = doc.getResultadoValidacao();

        return new ProcuracaoResumoDTO(
                doc.getId(),
                doc.getNomeArquivo(),
                doc.getTipoDocumento(),
                doc.getDataUpload(),
                d.getNumeroProcessoCnj(),
                d.getOutorganteNome(),
                d.getOutorgadoNome(),
                r != null ? r.getStatus() : null,
                r != null ? r.getJustificativas().size() : 0
        );
    }
}
