package br.org.cesar.tjpe.validador.controller;

import br.org.cesar.tjpe.validador.domain.enums.StatusValidacao;
import br.org.cesar.tjpe.validador.dto.CenarioTesteInfoDTO;
import br.org.cesar.tjpe.validador.dto.ProcuracaoDetalheResponseDTO;
import br.org.cesar.tjpe.validador.service.DocumentProcessingService;
import br.org.cesar.tjpe.validador.service.ocr.OcrService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller com os 3 Cenários Acadêmicos Obrigatórios exigidos pelo TJPE / CESAR School.
 */
@RestController
@RequestMapping("/api/testes")
public class CenariosTesteController {

    private final DocumentProcessingService processingService;

    public CenariosTesteController(DocumentProcessingService processingService) {
        this.processingService = processingService;
    }

    /**
     * Retorna a lista de cenários de teste disponíveis com seus resultados esperados.
     */
    @GetMapping("/cenarios")
    public ResponseEntity<List<CenarioTesteInfoDTO>> listarCenarios() {
        return ResponseEntity.ok(List.of(
                new CenarioTesteInfoDTO(
                        "cenario1",
                        "Cenário 1: Procuração Válida e Regular",
                        "Instrumento ad judicia completo, outorgante e outorgado regulares, OAB/PE ativa, assinatura Gov.br e poderes para o TJPE.",
                        StatusValidacao.APROVADO,
                        "Todos os requisitos formais e processuais foram atendidos."
                ),
                new CenarioTesteInfoDTO(
                        "cenario2",
                        "Cenário 2: Procuração com Prazo Vencido",
                        "Instrumento outorgado com cláusula expressa de validade expirada.",
                        StatusValidacao.REPROVADO,
                        "Instrumento de mandato expirado. Necessário apresentar nova procuração atualizada."
                ),
                new CenarioTesteInfoDTO(
                        "cenario3",
                        "Cenário 3: Procuração Ilegível ou com Ruído Excessivo",
                        "Digitalização corrompida, texto truncado ou borrões que impedem o OCR seguro.",
                        StatusValidacao.REVISAO_MANUAL,
                        "Documento de baixa legibilidade encaminhado para conferência manual da secretaria."
                )
        ));
    }

    /**
     * Dispara a execução completa de um dos três cenários obrigatórios.
     */
    @PostMapping("/cenario/{id}")
    public ResponseEntity<ProcuracaoDetalheResponseDTO> executarCenario(@PathVariable String id) throws IOException {
        MultipartFile file;

        switch (id.toLowerCase()) {
            case "cenario1":
            case "valida":
                file = new InMemoryMultipartFile(
                        "file",
                        "procuracao_valida_cenario1.pdf",
                        "application/pdf",
                        OcrService.CenarioOcrTextos.CENARIO_1_VALIDA.getBytes(StandardCharsets.UTF_8)
                );
                return ResponseEntity.ok(processingService.processarDocumento(file, "0001234-29.2024.8.17.0001"));

            case "cenario2":
            case "vencida":
                file = new InMemoryMultipartFile(
                        "file",
                        "procuracao_vencida_cenario2.pdf",
                        "application/pdf",
                        OcrService.CenarioOcrTextos.CENARIO_2_VENCIDA.getBytes(StandardCharsets.UTF_8)
                );
                return ResponseEntity.ok(processingService.processarDocumento(file, null));

            case "cenario3":
            case "ilegivel":
                file = new InMemoryMultipartFile(
                        "file",
                        "procuracao_ilegivel_cenario3.jpg",
                        "image/jpeg",
                        OcrService.CenarioOcrTextos.CENARIO_3_ILEGIVEL.getBytes(StandardCharsets.UTF_8)
                );
                return ResponseEntity.ok(processingService.processarDocumento(file, null));

            default:
                throw new IllegalArgumentException("Cenário desconhecido: " + id + ". Use: cenario1, cenario2 ou cenario3.");
        }
    }

    /**
     * Implementação autônoma de MultipartFile em memória para os cenários de teste.
     */
    public static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content != null ? content : new byte[0];
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }
}
