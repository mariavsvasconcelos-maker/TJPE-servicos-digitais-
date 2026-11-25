<div align="center">

# Validação Inteligente de Procuração Ad Judicia

**Caso:** TJPE (serviços digitais) · **Documento:** procuração ad judicia · **Autoria:** Maria Eduarda Vasconcelos  
**Residência Tecnológica:** CESAR School e Porto Digital 2026.2  
**Demonstração online:** [mariavsvasconcelos-maker.github.io/TJPE-servicos-digitais-](https://mariavsvasconcelos-maker.github.io/TJPE-servicos-digitais-/)

[![Java](https://img.shields.io/badge/Java-21-5382A1?style=flat-square)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?style=flat-square)](https://spring.io/projects/spring-boot)
[![Tesseract](https://img.shields.io/badge/Tesseract-OCR-5382A1?style=flat-square)](https://github.com/tesseract-ocr/tesseract)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-0288D1?style=flat-square)](https://www.postgresql.org/)
[![Testes](https://img.shields.io/badge/testes-15%20passando-2E7D58?style=flat-square)](#como-executar)

</div>

---

## Problema

Serviços do TJPE, como o peticionamento eletrônico, exigem o envio de procurações *ad judicia* conferidas manualmente por servidores do cartório. Isso gera filas, retrabalho e demora: o advogado envia o documento, aguarda e só depois descobre que foi rejeitado — muitas vezes sem entender o motivo.

A proposta é validar o documento no momento do protocolo, com feedback claro, reduzindo idas ao cartório e a carga de conferência manual.

**Caso real:** ao protocolar uma petição inicial, a procuração anexada ao PJe não teve a assinatura digital reconhecida, impedindo o vínculo com o processo. Como o prazo era de até 5 dias antes da audiência, foi necessário comparecer pessoalmente ao cartório para evitar a prorrogação.

## Usuários

| Persona | Necessidade |
| :--- | :--- |
| Advogado(a) | Enviar a procuração e saber imediatamente se está correta |
| Servidor(a) do cartório | Receber o documento já validado, com dados extraídos e motivos |
| Escritórios de advocacia | Enviar documentos em nome de vários clientes |
| Gestão do TJPE | Acompanhar volume, taxa de aprovação e motivos de recusa |

## Documentos e dados extraídos

O MVP processa a procuração *ad judicia* do TJPE em imagem (JPG/PNG) ou PDF e extrai 13 informações:

- **Processo:** número do processo no padrão CNJ (`NNNNNNN-DD.AAAA.J.TR.OOOO`).
- **Outorgante:** nome e CPF/CNPJ.
- **Outorgado (advogado):** nome, CPF, número de inscrição na OAB e seccional (padrão PE).
- **Procuração:** data de outorga, prazo de validade (vazio = prazo indeterminado), poderes concedidos e finalidade.
- **Assinatura:** se existe e de que tipo — digital (Gov.br / ICP-Brasil), física, ausente ou ilegível.
- **Carimbos e selos:** selos de cartório ou protocolos eletrônicos encontrados.

Campo não encontrado retorna `null` — a IA não inventa informações. O resultado chega em JSON, é convertido na classe `DadosExtraidos` (salva no banco) e devolvido pela API no record Java `DadosExtraidosDTO`.

**Evolução futura:** extrair também vara, comarca, classe processual, partes, assunto e tipo/número do documento de identificação; aceitar fotos de frente e verso; e processar outros documentos (subestabelecimento, declaração de hipossuficiência, RG e CNH) reutilizando o mesmo fluxo.

## Regras de validação

O motor de regras (`RegrasValidacaoService`) aplica 10 regras. Cada falha gera uma justificativa em linguagem simples.

| Nº | Regra | Se falhar |
| :---: | :--- | :--- |
| 1 | Documento legível (texto lido suficiente e sem degradação) | Revisão manual |
| 2 | Campos obrigatórios: nome e CPF/CNPJ do outorgante, nome e OAB do advogado | Reprovado |
| 3 | CPF/CNPJ com dígitos verificadores corretos (módulo 11) | Reprovado |
| 4 | Número do processo CNJ com dígito verificador correto (módulo 97, Resolução CNJ nº 65/2008) e do TJPE (`.8.17`) | Reprovado se o dígito for inválido; revisão manual se não for do TJPE |
| 5 | Processo localizado no PJe *(consulta simulada)* | Revisão manual |
| 6 | Assinatura presente | Reprovado se ausente; revisão manual se ilegível |
| 7 | Procuração dentro da validade (prazo indeterminado é aceito, art. 682 do Código Civil) | Reprovado |
| 8 | Advogado com inscrição regular na OAB *(consulta simulada ao CNA)* | Reprovado |
| 9 | Poderes *ad judicia* / foro em geral | Reprovado se não houver poderes; revisão manual se faltar menção a *ad judicia* |
| 10 | Data de outorga identificada e não futura | Reprovado |

As regras 4 e 5 só são aplicadas quando a procuração cita um número de processo (ou ele é informado no envio). Procurações gerais para o foro, sem processo, são aceitas.

> **Integrações simuladas:** o acesso real ao PJe e ao Cadastro Nacional dos Advogados (CNA/OAB) exige credenciais governamentais. Nesta versão, `PjeIntegrationClient` e `CnaOabIntegrationClient` já têm a interface pronta, mas devolvem respostas simuladas e marcadas com o aviso `DEMONSTRAÇÃO ACADÊMICA CESAR SCHOOL`.

## Aprovação, reprovação e revisão manual

**Aprovado** quando todas as regras são atendidas: dados completos e válidos, procuração assinada e vigente, OAB regular e documento legível. O status é salvo no banco e devolvido pela API; a petição segue sem conferência manual.

**Reprovado** quando há um problema objetivo: campo obrigatório ausente, CPF/CNPJ ou número do processo com dígito inválido, procuração sem assinatura ou vencida, OAB irregular, ausência de poderes ou data de outorga ausente/futura. A resposta traz o motivo em linguagem simples (ex.: "A assinatura não foi identificada. Envie uma nova procuração com a assinatura legível.").

**Revisão manual** quando não há reprovação, mas há dúvida que o sistema não deve decidir sozinho: documento ilegível, assinatura ilegível, processo de outro tribunal ou não localizado no PJe, ou poderes sem menção expressa a *ad judicia*. O documento é encaminhado para um servidor.

Se houver motivos de reprovação e de revisão ao mesmo tempo, prevalece a reprovação.

## Fluxo de Processamento (Pipeline)

O documento passa por seis etapas, orquestradas por `DocumentProcessingService`:

1. **Upload:** o usuário envia a procuração (JPG, PNG ou PDF) pela interface web, no computador ou no celular.
2. **Pré-processamento da imagem (Java ImageIO/AWT):** converte para tons de cinza, ajusta o contraste, binariza pelo método de Otsu e estima o nível de ruído. A correção de rotação já tem a estrutura pronta, mas a detecção do ângulo ainda não foi implementada. A correção de perspectiva e o uso do OpenCV estão planejados.
3. **OCR (Tesseract):** extrai o texto com os idiomas português e inglês, chamando o Tesseract instalado na máquina. Se ele não estiver no PATH, entra um modo de contingência que lê o texto embutido no arquivo (ou os textos de exemplo dos cenários de teste).
4. **IA / LLM:** interpreta o texto e estrutura as informações em JSON, retornando `null` para o que não existir. Por padrão roda um interpretador local baseado em padrões de texto, sem internet e sem custo. Opcionalmente, usa um LLM por API compatível com o formato Chat Completions da OpenAI (OpenAI, ou Gemini e Ollama pelos seus endpoints compatíveis).
5. **Validação (Spring Boot):** aplica as 10 regras de negócio descritas acima.
6. **Persistência e retorno:** salva no PostgreSQL o documento (nome, tipo, tamanho e hash SHA-256), o texto do OCR, os dados extraídos e o parecer, e devolve na hora o status com as justificativas.

## Estrutura do Projeto

A aplicação Java fica em `src/main/java/br/org/cesar/tjpe/validador/`:

```
validador/
├── ValidadorProcuracoesApplication.java   # Ponto de entrada do Spring Boot
├── config/        # ObjectMapper/RestTemplate, CORS e tratamento global de erros
├── controller/    # ProcuracaoController (upload e histórico) e CenariosTesteController (3 cenários)
├── domain/
│   ├── enums/     # StatusValidacao, TipoDocumento, TipoAssinatura
│   └── model/     # Entidade ProcuracaoDocumento, DadosExtraidos e ResultadoValidacao
├── dto/           # Records de resposta da API (DadosExtraidosDTO, ResultadoValidacaoDTO etc.)
├── repository/    # ProcuracaoDocumentoRepository (Spring Data JPA)
└── service/
    ├── DocumentProcessingService.java     # Orquestra o pipeline
    ├── image/        # ImagePreprocessingService — tratamento da imagem
    ├── ocr/          # OcrService — Tesseract e modo de contingência
    ├── llm/          # LlmExtractionService e PromptTemplates — extração em JSON
    ├── validator/    # RegrasValidacaoService, CpfCnpjValidator, CnjValidator
    └── integration/  # PjeIntegrationClient e CnaOabIntegrationClient (simulados)
```

Outros arquivos importantes:

- `src/main/resources/static/` — interface web (HTML, CSS e JavaScript).
- `src/main/resources/samples/` — textos de exemplo dos 3 cenários.
- `src/main/resources/application.yml` e `application-h2.yml` — configuração do PostgreSQL e do banco H2 em memória.
- `docker-compose.yml` — sobe o PostgreSQL 16.
- `ABRIR_SITE.html` — demonstração visual que abre direto no navegador, sem servidor (os resultados nela são simulados no próprio navegador).

### API REST

| Método | Endpoint | Função |
| :--- | :--- | :--- |
| `POST` | `/api/procuracoes/processar` | Recebe o arquivo (`file`) e, opcionalmente, o `numeroProcessoCnj`; devolve dados extraídos e parecer |
| `GET` | `/api/procuracoes` | Lista o histórico de validações |
| `GET` | `/api/procuracoes/{id}` | Detalha uma validação |
| `GET` | `/api/testes/cenarios` | Lista os 3 cenários de demonstração |
| `POST` | `/api/testes/cenario/{id}` | Executa um cenário (`cenario1`, `cenario2` ou `cenario3`) |

## Escopo do MVP (Produto Mínimo Viável)

O MVP entrega o fluxo completo de ponta a ponta — do envio do documento pelo advogado até a resposta automática da validação — dividido em 4 pilares principais:

**1. Interface do Usuário (Front-end responsivo)**

- Tela responsiva para celular e desktop, com a identidade visual do TJPE (azul-marinho e dourado).
- Permite anexar a procuração (JPG, PNG ou PDF); no celular, o navegador permite escolher uma foto da galeria ou da câmera.
- Exibe imediatamente o resultado da validação e os motivos detalhados em caso de pendência.
- Botões para rodar os 3 cenários de demonstração com um clique.

**2. Processamento Inteligente (Pipeline de Dados)**

- Tratamento de imagem (Java ImageIO/AWT): tons de cinza, contraste e binarização de Otsu.
- Extração de texto (Tesseract OCR): converte a imagem tratada em texto bruto.
- Estruturação com IA: interpreta o texto e gera um JSON padronizado com os dados do documento.

**3. Back-end e Regras de Negócio (Spring Boot + PostgreSQL)**

- API REST: endpoint `POST /api/procuracoes/processar` para recebimento e análise dos arquivos.
- Motor de validação em Java: aplica as 10 regras (campos obrigatórios, CPF/CNPJ, número CNJ, PJe, assinatura, validade, OAB, poderes e data de outorga).
- Banco de dados: persistência dos dados extraídos, status final e justificativas no PostgreSQL (ou H2 em memória, para testes e demonstração).

**4. Cenários de Demonstração (Prova de Conceito)**

O sistema é validado com três cenários, montados com documentos de exemplo:

- **Cenário 1 — Aprovado:** procuração legível, assinada e com todos os dados válidos.
- **Cenário 2 — Reprovado por prazo:** documento legível, porém com validade expirada em 15/03/2023.
- **Cenário 3 — Revisão manual por qualidade:** documento ilegível, que impede a leitura segura; o sistema não decide sozinho e encaminha para um servidor.

## Como executar

**Pré-requisitos:** Java 21 e Maven 3.9+. Opcionais: Docker (para o PostgreSQL) e Tesseract com o idioma português instalado no PATH. No Windows, `.\mvnw.cmd` pode ser usado no lugar de `mvn`.

**Jeito mais fácil (Windows):** na pasta do projeto, rode o comando abaixo (ou dê dois cliques no arquivo). Ele liga o sistema e abre o navegador sozinho quando estiver pronto:

```powershell
.\abrir-site.cmd
```

Para desligar, feche a janela "TJPE - Servidor".

**Rodar os testes** (15 testes: CPF/CNPJ, número CNJ, regras, os 3 cenários de ponta a ponta — do documento ao parecer — e a inicialização da aplicação):

```bash
mvn test
```

**Rodar a aplicação sem Docker** (banco H2 em memória):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

**Rodar com PostgreSQL:**

```bash
docker compose up -d
mvn spring-boot:run
```

Depois, abra **http://localhost:8080**. Os cenários também podem ser disparados pelo terminal:

```bash
curl -X POST http://localhost:8080/api/testes/cenario/cenario1
```

**Usar um LLM externo (opcional):** defina as variáveis de ambiente `TJPE_LLM_PROVIDER` (`openai`, `gemini` ou `ollama`), `TJPE_LLM_API_KEY`, `TJPE_LLM_MODEL` (padrão `gpt-4o-mini`) e, para Gemini ou Ollama, `TJPE_LLM_API_URL` com o endpoint compatível com Chat Completions. Sem chave, o sistema usa o interpretador local.

## Limitações atuais e próximos passos

- **PJe e OAB:** as consultas são simuladas; o próximo passo é a integração real, com as credenciais do tribunal.
- **PDF:** o sistema ainda não converte páginas de PDF em imagem; de um PDF, ele aproveita apenas o texto legível embutido no arquivo.
- **Imagem:** a correção automática de rotação e de perspectiva ainda não foi implementada.
- **Dados reais:** os cenários usam documentos de exemplo; o próximo passo é testar com procurações reais do TJPE.
