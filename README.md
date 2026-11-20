# Sistema de Validação Inteligente de Procurações Ad Judicia — TJPE & CESAR School

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x%20%2F%204.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![OpenCV](https://img.shields.io/badge/OpenCV-Image%20Processing-red.svg)](https://opencv.org/)
[![Tesseract OCR](https://img.shields.io/badge/Tesseract-OCR-yellow.svg)](https://github.com/tesseract-ocr/tesseract)
[![Status](https://img.shields.io/badge/Status-100%25%20Funcional-success.svg)](#)

Projeto acadêmico completo e funcional desenvolvido para a **CESAR School** em colaboração com o **Tribunal de Justiça do Estado de Pernambuco (TJPE)**.

O objetivo da plataforma é receber procurações forenses em formato digital (PDF ou imagens JPG/PNG), realizar o tratamento avançado das imagens com **OpenCV**, extrair a camada de texto com **Tesseract OCR**, interpretar os elementos semânticos e extrair dados estruturados em JSON via **Inteligência Artificial (LLM)**, aplicar regras rigorosas de validação jurídica e processual em **Java / Spring Boot** e persistir todo o histórico e pareceres no banco de dados **PostgreSQL**.

---

## 1. Visão Geral da Arquitetura e Fluxo do Sistema

```
[ Advogado / Usuário ]
          │
          ▼ 1. Upload de Procuração (PDF / PNG / JPG)
┌────────────────────────────────────────────────────────┐
│               Spring Boot REST Controller              │
└──────────────────────────┬─────────────────────────────┘
                           │ 2. Pipeline de Processamento
                           ▼
┌────────────────────────────────────────────────────────┐
│     ETAPA 1: Tratamento de Imagem (OpenCV / AWT)       │
│  - Conversão em Escala de Cinza & Remoção de Ruídos    │
│  - Correção de Rotação e Perspectiva (Deskew)          │
│  - Binarização Adaptativa pelo Método de Otsu          │
└──────────────────────────┬─────────────────────────────┘
                           │ 3. Imagem Otimizada
                           ▼
┌────────────────────────────────────────────────────────┐
│          ETAPA 2: Extração com Tesseract OCR           │
│  - Reconhecimento óptico de caracteres em Português    │
│  - Tratamento de caracteres e legibilidade             │
└──────────────────────────┬─────────────────────────────┘
                           │ 4. Texto Bruto
                           ▼
┌────────────────────────────────────────────────────────┐
│        ETAPA 3: Interpretação Semântica com IA         │
│  - LLM com Prompt Engineering estruturado para JSON    │
│  - Campos ausentes retornam estritamente "null"        │
└──────────────────────────┬─────────────────────────────┘
                           │ 5. Dados Estruturados em JSON
                           ▼
┌────────────────────────────────────────────────────────┐
│      ETAPA 4: Motor de Regras de Validação TJPE        │
│  - Verificação de Campos Obrigatórios                  │
│  - Cálculo de Dígitos Verificadores CPF/CNPJ (Mód. 11) │
│  - Validação de Numeração Única CNJ (Módulo 97)        │
│  - Validação de Validade Temporal & Poderes Forenses   │
│  - Simulação identificada de Barramento PJe e CNA/OAB  │
└──────────────────────────┬─────────────────────────────┘
                           │ 6. Parecer Final e Justificativas
                           ▼
┌────────────────────────────────────────────────────────┐
│        ETAPA 5: Persistência no PostgreSQL             │
│  - Armazenamento do Documento, OCR, JSON e Resultado   │
│  - Interface Web em Tempo Real (Desktop e Mobile)      │
└────────────────────────────────────────────────────────┘
```

---

## 2. Tecnologias Obrigatórias e Justificativas Técnicas

| Tecnologia | Função no Sistema | Detalhes de Implementação |
| :--- | :--- | :--- |
| **Java 21 LTS** | Linguagem Principal | Utiliza records, pattern matching, APIs de concorrência e tipos imutáveis. |
| **Spring Boot** | Framework Backend & API REST | Arquitetura limpa em camadas (Controller, Service, Repository, DTO). |
| **OpenCV / ImageIO** | Tratamento e Limpeza Gráfica | Escala de cinza, deskew, aumento de contraste e binarização Otsu para OCR nítido. |
| **Tesseract OCR** | Reconhecimento de Caracteres | Extração óptica do texto com dicionário em português e fallback acadêmico resiliente. |
| **Inteligência Artificial (LLM)** | Interpretação Semântica | Extrator com schema estrito em JSON. Suporta OpenAI, Gemini, Ollama ou Motor Heurístico/NLP local autônomo. |
| **PostgreSQL 16** | Banco de Dados Relacional | Persistência de documentos, metadados, hash SHA-256, texto OCR e resultados dos pareceres. |
| **H2 Database** | Banco em Memória de Fallback | Permite que a aplicação execute e passe em 100% dos testes sem exigir Docker de imediato. |
| **HTML5 / CSS3 / Vanilla JS** | Interface Web do Advogado | Interface intuitiva, moderna e responsiva (adaptada para desktop e celulares). |

---

## 3. Informações Extraídas (Conforme Item 4)

Caso a IA não consiga identificar alguma informação com certeza, o valor retorna **estritamente `null`**, sem inventar dados:

1. **Número do processo (CNJ)**: Formato `NNNNNNN-DD.AAAA.J.TR.OOOO`.
2. **Nome do outorgante**: Pessoa física ou jurídica.
3. **CPF ou CNPJ do outorgante**: Validado matematicamente.
4. **Nome do outorgado**: Advogado constituído.
5. **CPF do outorgado**: Documento do profissional.
6. **Número de inscrição na OAB**: Registro profissional.
7. **Seccional da OAB**: UF de registro (padrão PE).
8. **Data de outorga**: Data da assinatura/emissão do mandato.
9. **Prazo de validade**: Prazo final ou `null` (indeterminado segundo art. 682 do Código Civil).
10. **Poderes concedidos**: Cláusula *ad judicia*, foro em geral e poderes especiais.
11. **Finalidade da procuração**: Descrição do ato ou processo judicial.
12. **Assinatura**: Identificação de assinatura física, digital (Gov.br / ICP-Brasil), ausente ou ilegível.
13. **Carimbos e selos**: Selos de cartório ou protocolos eletrônicos do TJPE.

---

## 4. As 10 Regras de Validação do TJPE (Conforme Item 5)

| Nº | Regra de Negócio | Ação do Sistema | Justificativa em Caso de Falha |
| :---: | :--- | :---: | :--- |
| **1** | **Legibilidade e Integridade** | REVISÃO / REPROVADO | *"O documento digitalizado apresenta baixa legibilidade ou ruído excessivo."* |
| **2** | **Presença de Campos Obrigatórios** | REPROVADO | *"O nome/CPF do outorgante ou outorgado/OAB não foi identificado no documento."* |
| **3** | **Dígitos Verificadores CPF/CNPJ** | REPROVADO | *"O CPF/CNPJ informado possui dígito verificador inválido (Módulo 11)."* |
| **4** | **Número do Processo (CNJ)** | REPROVADO | *"O processo possui dígito verificador incorreto (Resolução CNJ nº 65/2008)."* |
| **5** | **Correspondência com o PJe TJPE** | REVISÃO MANUAL | *"DEMONSTRAÇÃO ACADÊMICA: Processo não localizado na base do PJe TJPE."* |
| **6** | **Existência da Assinatura** | REPROVADO | *"A assinatura não foi identificada. Envie uma nova procuração com a assinatura legível."* |
| **7** | **Validade da Procuração** | REPROVADO | *"A procuração encontra-se com o prazo de validade expirado desde DD/MM/AAAA."* |
| **8** | **Situação Cadastral na OAB** | REPROVADO | *"A inscrição do advogado na OAB consta como suspensa ou cancelada no CNA."* |
| **9** | **Poderes Ad Judicia** | REVISÃO MANUAL | *"O instrumento não contém a cláusula 'ad judicia' ou foro em geral."* |
| **10**| **Data de Outorga e Prazos** | REPROVADO | *"A data de outorga indicada é futura em relação à data atual."* |

> **Nota sobre Integrações Externas (Item 5 do Edital):**
> Na ausência de barramento mTLS governamental em produção, os clientes de integração (`PjeIntegrationClient` e `CnaOabIntegrationClient`) foram desenvolvidos com a interface arquitetural completa, operando em **modo simulado com aviso acadêmico explícito**:  
> `[DEMONSTRAÇÃO ACADÊMICA CESAR SCHOOL: Consulta simulada ao PJe TJPE / CNA OAB]`.

---

## 5. Estrutura do Projeto

```
c:\Users\vasconcelos\Downloads\TJPE - PORTO DIGITAL
├── docker-compose.yml                     # Subida do PostgreSQL 16
├── pom.xml                                # Gerenciador de dependências Maven
├── mvnw.cmd / mvnw                        # Maven Wrapper autônomo
├── README.md                              # Documentação acadêmica
├── src/
│   ├── main/
│   │   ├── java/br/org/cesar/tjpe/validador/
│   │   │   ├── ValidadorProcuracoesApplication.java   # Main Spring Boot
│   │   │   ├── config/
│   │   │   │   ├── AppConfig.java                    # Beans ObjectMapper e RestTemplate
│   │   │   │   ├── CorsConfig.java                   # Habilitação CORS para APIs REST
│   │   │   │   └── GlobalExceptionHandler.java       # Tratamento global de erros HTTP
│   │   │   ├── controller/
│   │   │   │   ├── ProcuracaoController.java         # Endpoints REST de upload e histórico
│   │   │   │   └── CenariosTesteController.java      # Disparo dos 3 cenários de teste em 1 clique
│   │   │   ├── domain/
│   │   │   │   ├── enums/
│   │   │   │   │   ├── StatusValidacao.java          # APROVADO, REPROVADO, REVISAO_MANUAL
│   │   │   │   │   ├── TipoDocumento.java            # PDF, IMAGEM_JPG, IMAGEM_PNG
│   │   │   │   │   └── TipoAssinatura.java           # DIGITAL, FISICA, AUSENTE, ILEGIVEL
│   │   │   │   └── model/
│   │   │   │       ├── ProcuracaoDocumento.java      # Entidade JPA principal
│   │   │   │       ├── DadosExtraidos.java           # Embeddable com os 11 campos
│   │   │   │       └── ResultadoValidacao.java       # Parecer e justificativas do TJPE
│   │   │   ├── dto/
│   │   │   │   ├── DadosExtraidosDTO.java            # Transporte dos campos extraídos
│   │   │   │   ├── ResultadoValidacaoDTO.java        # Transporte do parecer
│   │   │   │   ├── ProcuracaoDetalheResponseDTO.java # Resposta detalhada
│   │   │   │   └── ProcuracaoResumoDTO.java          # Linha do histórico
│   │   │   ├── repository/
│   │   │   │   └── ProcuracaoDocumentoRepository.java # Consultas Spring Data JPA
│   │   │   └── service/
│   │   │       ├── DocumentProcessingService.java    # Orquestrador do pipeline
│   │   │       ├── image/
│   │   │       │   └── ImagePreprocessingService.java # OpenCV: deskew, Otsu e ruídos
│   │   │       ├── ocr/
│   │   │       │   └── OcrService.java               # Tesseract OCR com fallback
│   │   │       ├── llm/
│   │   │       │   ├── LlmExtractionService.java     # Extrator semântico JSON
│   │   │       │   └── PromptTemplates.java          # Prompts de engenharia jurídica
│   │   │       ├── validator/
│   │   │       │   ├── RegrasValidacaoService.java   # Motor das 10 regras TJPE
│   │   │       │   ├── CpfCnpjValidator.java         # Módulo 11 oficial da Receita
│   │   │       │   └── CnjValidator.java             # Módulo 97 da Resolução CNJ 65/2008
│   │   │       └── integration/
│   │   │           ├── PjeIntegrationClient.java     # Simulação PJe com alerta acadêmico
│   │   │           └── CnaOabIntegrationClient.java  # Simulação CNA/OAB com alerta acadêmico
│   │   └── resources/
│   │       ├── application.yml                       # Configuração PostgreSQL e TJPE
│   │       ├── application-h2.yml                    # Perfil H2 em memória
│   │       ├── samples/                              # Documentos dos 3 cenários
│   │       └── static/                               # Interface Web (HTML5/CSS3/JS)
│   │           ├── index.html                        # Tela do advogado
│   │           ├── css/style.css                     # Estilização institucional TJPE
│   │           └── js/app.js                         # Lógica AJAX e renderização
│   └── test/
│       └── java/br/org/cesar/tjpe/validador/
│           ├── ValidadorProcuracoesApplicationTests.java # Teste de carregamento do Spring
│           └── service/validator/
│               ├── CpfCnpjValidatorTest.java         # Testes de CPF e CNPJ
│               ├── CnjValidatorTest.java             # Testes de Módulo 97 CNJ
│               └── RegrasValidacaoServiceTest.java   # Teste dos 3 Cenários Obrigatórios
```

---

## 6. Como Executar a Aplicação

### Pré-requisitos
- **Java 21 LTS** instalado (`java -version`).
- **Docker** (opcional, para rodar o PostgreSQL via container).

### Passo 1: Subir o PostgreSQL (Opcional)
Se desejar persistir no PostgreSQL oficial via Docker:
```bash
docker compose up -d
```
> *Nota: Caso prefira rodar sem Docker de imediato, o perfil `h2` integrado funciona instantaneamente sem configurações adicionais.*

### Passo 2: Executar os Testes Automatizados
Todos os 10 testes unitários cobrindo as regras do TJPE, CPF/CNPJ, CNJ e os 3 cenários obrigatórios rodam com:
```powershell
.\mvnw.cmd test -Dtest=RegrasValidacaoServiceTest,CpfCnpjValidatorTest,CnjValidatorTest -o
```

### Passo 3: Iniciar a Aplicação Spring Boot
Execute no terminal:
```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2 -o
```
*(Para conectar diretamente ao container PostgreSQL, basta executar `.\mvnw.cmd spring-boot:run`)*.

### Passo 4: Acessar a Interface do Advogado
Abra no navegador:
👉 **[http://localhost:8080](http://localhost:8080)**

---

## 7. Demonstração dos 3 Cenários Obrigatórios de Teste (Item 8)

A interface gráfica possui botões dedicados de **1 Clique** para testar os 3 cenários:

| Cenário | Descrição | Status Retornado | Justificativa Gerada |
| :---: | :--- | :---: | :--- |
| **Cenário 1** | **Procuração Válida e Regular** | `APROVADO` | *"Procuração em conformidade com as exigências processuais do TJPE. Poderes ad judicia regulares, outorgante e outorgado identificados com documentos válidos e assinatura verificada."* |
| **Cenário 2** | **Procuração com Prazo Vencido** | `REPROVADO` | *"A procuração encontra-se com o prazo de validade expirado desde 15/03/2023. É necessário apresentar novo instrumento de mandato com poderes vigentes."* |
| **Cenário 3** | **Imagem Ilegível / Ruído Excessivo** | `REVISÃO MANUAL` | *"O documento digitalizado apresenta baixa legibilidade, corte ou borrões que impedem a leitura segura do conteúdo. Encaminhado para verificação manual da secretaria judiciária."* |

### Testando os Cenários via Terminal (cURL / PowerShell)

**Cenário 1 (Válida):**
```bash
curl -X POST http://localhost:8080/api/testes/cenario/cenario1
```

**Cenário 2 (Vencida):**
```bash
curl -X POST http://localhost:8080/api/testes/cenario/cenario2
```

**Cenário 3 (Ilegível):**
```bash
curl -X POST http://localhost:8080/api/testes/cenario/cenario3
```

---

## 8. Integração com LLMs Comerciais (Opcional)

Por padrão, o sistema opera no modo autônomo com o **Motor Heurístico/NLP Especializado** para garantir 100% de disponibilidade sem custos ou rate-limit. Se desejar conectar a uma LLM remota (ex: OpenAI GPT-4o-mini ou Google Gemini):

No arquivo `src/main/resources/application.yml` ou via variáveis de ambiente:
```yaml
tjpe:
  validador:
    llm:
      provider: openai # ou gemini | ollama
      api-key: SUA_CHAVE_AQUI
      model: gpt-4o-mini
```

---

## 9. Autoria e Contexto Institucional
- **Instituição:** CESAR School
- **Tribunal Parceiro:** Tribunal de Justiça de Pernambuco (TJPE)
- **Disciplina:** Residência em Engenharia de Software / Projetos Tecnológicos
