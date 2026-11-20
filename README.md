# Validação Inteligente de Procuração ad Judicia

**Caso:** TJPE (serviços digitais) · **Documento:** procuração ad judicia · **Autoria:** Maria Eduarda Vasconcelos — CESAR School, ADS Regular 2026.2 · Residência, etapa Grow Up
![Java](https://img.shields.io/badge/Java-21-informational?style=flat-square)![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-informational?style=flat-square)![OpenCV](https://img.shields.io/badge/OpenCV-visão%20computacional-informational?style=flat-square)![Tesseract](https://img.shields.io/badge/Tesseract-OCR-informational?style=flat-square)![PostgreSQL](https://img.shields.io/badge/PostgreSQL-persistência-informational?style=flat-square)
## Problema
Serviços do TJPE, como o peticionamento eletrônico, exigem o envio de procurações *ad judicia* conferidas manualmente por servidores do cartório. Isso gera filas, retrabalho e demora: o advogado envia o documento, aguarda e só depois descobre que foi rejeitado — muitas vezes sem entender o motivo.
A proposta é validar o documento no momento do protocolo, com feedback claro, reduzindo idas ao cartório e a carga de conferência manual.
**Caso real:** ao protocolar uma petição inicial, a procuração anexada ao PJe não teve a assinatura digital reconhecida, impedindo o vínculo com o processo. Como o prazo era de até 5 dias antes da audiência, foi necessário comparecer pessoalmente ao cartório para evitar a prorrogação.
## Usuários
| Persona | Necessidade || :--- | :--- || Advogado(a) | Enviar a procuração e saber imediatamente se está correta || Servidor(a) do cartório | Receber o documento já validado, com dados extraídos e motivos || Escritórios de advocacia | Enviar documentos em nome de vários clientes || Gestão do TJPE | Acompanhar volume, taxa de aprovação e motivos de recusa |
## Documentos e dados extraídos
O MVP processa a procuração ad judicia do TJPE em imagem (JPG/PNG) ou PDF. Evolução futura: subestabelecimento, declaração de hipossuficiência, RG e CNH, reutilizando o mesmo fluxo.
- **Procuração:** número do processo (CNJ), vara, comarca, data de outorga, prazo de validade, poderes conferidos, status de assinatura.- **Outorgante e outorgado:** nome, CPF/CNPJ, tipo e número do documento, inscrição na OAB.- **Processo:** número CNJ, classe processual, vara/comarca, partes, assunto.- **Coletado no documento:** assinatura (física ou digital), carimbo/selo, fotos de frente e verso.- **Finalidade e OAB:** finalidade processual, inscrição, seccional e nome do advogado.
Campo não encontrado retorna `null` — a IA não inventa informações. O resultado chega em JSON e é desserializado em um `record` Java (`DadosProcuracao`).
## Fluxo
```text1. Documento    Upload no portal ou celular2. OpenCV       Correção de rotação, perspectiva, contraste e ruído3. Tesseract    OCR: extrai o texto4. IA (LLM)     Interpreta o texto e devolve JSON5. Spring Boot  Valida os dados e aplica as regras6. Resultado    Persiste no PostgreSQL e retorna o status```
Documentos reprovados registram o motivo e podem ser encaminhados à revisão manual de um servidor.
## Regras de validação
1. Campos obrigatórios presentes (processo, CPF/CNPJ, OAB, datas, poderes).2. Documento assinado e, quando aplicável, dentro da validade.3. CPF/CNPJ com formato válido e dígitos verificadores corretos (regex + cálculo em Java).4. Número do processo (CNJ) e partes conferem com o processo no PJe.5. Finalidade compatível com o ato processual e OAB ativa.6. Protocolo dentro do prazo aplicável, documento legível, com assinatura e carimbo.
## Aprovação e rejeição
**Aprovado** quando todas as regras são atendidas: dados conferem com o PJe, procuração assinada e válida, OAB ativa, prazo respeitado e documento legível. O status é salvo no banco e devolvido pela API; a petição segue sem conferência manual.
**Rejeitado** quando há campo obrigatório ausente ou CPF/CNPJ/OAB inválido, processo divergente do protocolo, procuração sem assinatura ou vencida, prazo estourado, OAB inativa, imagem ilegível após tratamento ou documento que não é uma procuração. A resposta traz o motivo em linguagem simples (ex.: "a assinatura não foi encontrada, envie outra"); casos duvidosos vão para revisão.
## MVP
- Tela de envio mobile-first: o advogado fotografa ou anexa o PDF e vê o status na hora.- API REST em Spring Boot com `POST /documentos/validar`.- Pipeline OpenCV → Tesseract → IA (JSON).- Validação em Java das regras principais.- Persistência no PostgreSQL e retorno do status com motivos.- Demonstração final com três exemplos: procuração aprovada, vencida e foto ilegível.
## Estrutura do projeto
```textsrc/main/java/br/tjpe/procuracao/├── api/           Controllers REST├── domain/        DadosProcuracao, Parte, enums de status├── validation/    Regras, validador de CPF/CNPJ, OAB├── ocr/           Integração OpenCV + Tesseract├── ai/            Cliente do LLM (texto para JSON)└── persistence/   Entidades e repositórios```
## Roadmap
- [x] Definição do problema, usuários e regras- [x] Desenho do fluxo e do contrato de dados- [ ] Pipeline OpenCV + Tesseract- [ ] Integração com LLM e `DadosProcuracao`- [ ] Motor de regras em Java- [ ] Persistência e API REST- [ ] Front-end mobile-first
