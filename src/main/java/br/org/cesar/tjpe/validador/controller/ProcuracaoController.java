package br.org.cesar.tjpe.validador.controller;

import br.org.cesar.tjpe.validador.dto.ProcuracaoDetalheResponseDTO;
import br.org.cesar.tjpe.validador.dto.ProcuracaoResumoDTO;
import br.org.cesar.tjpe.validador.service.DocumentProcessingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Controller REST principal para recepção, processamento e histórico de procurações judiciais.
 */
@RestController
@RequestMapping("/api/procuracoes")
public class ProcuracaoController {

    private final DocumentProcessingService processingService;

    public ProcuracaoController(DocumentProcessingService processingService) {
        this.processingService = processingService;
    }

    /**
     * Endpoint para upload e validação de procurações em PDF, PNG ou JPG.
     */
    @PostMapping(value = "/processar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProcuracaoDetalheResponseDTO> processarProcuracao(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "numeroProcessoCnj", required = false) String numeroProcessoCnj) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo de procuração não pode estar vazio.");
        }

        ProcuracaoDetalheResponseDTO response = processingService.processarDocumento(file, numeroProcessoCnj);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para consulta de uma validação específica por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProcuracaoDetalheResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(processingService.buscarPorId(id));
    }

    /**
     * Endpoint para listagem de histórico das procurações processadas.
     */
    @GetMapping
    public ResponseEntity<List<ProcuracaoResumoDTO>> listarHistorico() {
        return ResponseEntity.ok(processingService.listarHistorico());
    }
}
