# Validação Inteligente de Procuração ad Judicia

**Caso:** TJPE (serviços digitais) · **Documento:** procuração ad judicia · 
**Autoria:** Maria Eduarda Vasconcelos — CESAR School, ADS Regular 2026.2 · Residência, etapa Grow Up
![Java](https://img.shields.io/badge/Java-21-informational?style=flat-square)![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-informational?style=flat-square)![OpenCV](https://img.shields.io/badge/OpenCV-visão%20computacional-informational?style=flat-square)![Tesseract](https://img.shields.io/badge/Tesseract-OCR-informational?style=flat-square)![PostgreSQL](https://img.shields.io/badge/PostgreSQL-persistência-informational?style=flat-square)

## Problema
Serviços do TJPE, como o peticionamento eletrônico, exigem o envio de procurações *ad judicia* conferidas manualmente por servidores do cartório. Isso gera filas, retrabalho e demora: o advogado envia o documento, aguarda e só depois descobre que foi rejeitado — muitas vezes sem entender o motivo.
A proposta é validar o documento no momento do protocolo, com feedback claro, reduzindo idas ao cartório e a carga de conferência manual.
**Caso real:** ao protocolar uma petição inicial, a procuração anexada ao PJe não teve a assinatura digital reconhecida, impedindo o vínculo com o processo. Como o prazo era de até 5 dias antes da audiência, foi necessário comparecer pessoalmente ao cartório para evitar a prorrogação.

## Usuários
|| Persona e Necessidade:
|| Advogado(a) | Enviar a procuração e saber imediatamente se está correta 
|| Servidor(a) do cartório | Receber o documento já validado, com dados extraídos e motivos 
|| Escritórios de advocacia | Enviar documentos em nome de vários clientes 
|| Gestão do TJPE | Acompanhar volume, taxa de aprovação e motivos de recusa |

## Documentos e dados extraídos
O MVP processa a procuração ad judicia do TJPE em imagem (JPG/PNG) ou PDF. Evolução futura: subestabelecimento, declaração de hipossuficiência, RG e CNH, reutilizando o mesmo fluxo.
- **Procuração:** número do processo (CNJ), vara, comarca, data de outorga, prazo de validade, poderes conferidos, status de assinatura.- **Outorgante e outorgado:** nome, CPF/CNPJ, tipo e número do documento, inscrição na OAB.- **Processo:** número CNJ, classe processual, vara/comarca, partes, assunto.- **Coletado no documento:** assinatura (física ou digital), carimbo/selo, fotos de frente e verso.- **Finalidade e OAB:** finalidade processual, inscrição, seccional e nome do advogado.
Campo não encontrado retorna `null` — a IA não inventa informações. O resultado chega em JSON e é desserializado em um `record` Java (`DadosProcuracao`).

## Regras de validação
1. Campos obrigatórios presentes (processo, CPF/CNPJ, OAB, datas, poderes).
2. Documento assinado e, quando aplicável, dentro da validade.
3. CPF/CNPJ com formato válido e dígitos verificadores corretos (regex + cálculo em Java).
4. Número do processo (CNJ) e partes conferem com o processo no PJe.
5. Finalidade compatível com o ato processual e OAB ativa.
6. Protocolo dentro do prazo aplicável, documento legível, com assinatura e carimbo.

## Aprovação e rejeição 

**Aprovado** quando todas as regras são atendidas: dados conferem com o PJe, procuração assinada e válida, OAB ativa, prazo respeitado e documento legível. O status é salvo no banco e devolvido pela API; a petição segue sem conferência manual.

**Rejeitado** quando há campo obrigatório ausente ou CPF/CNPJ/OAB inválido, processo divergente do protocolo, procuração sem assinatura ou vencida, prazo estourado, OAB inativa, imagem ilegível após tratamento ou documento que não é uma procuração. A resposta traz o motivo em linguagem simples (ex.: "a assinatura não foi encontrada, envie outra"); casos duvidosos vão para revisão.

## Fluxo de Processamento (Pipeline)
O documento passa por seis etapas sequenciais desde o envio até a resposta final:
1. Upload: O usuário envia a procuração (imagem JPG/PNG ou PDF) pelo celular ou portal web.
2. OpenCV (Pré-processamento): Corrige rotação, perspectiva, contraste e ruído para preparar a imagem.
3. Tesseract (OCR): Extrai todo o texto bruto contido no documento tratado.
4. IA / LLM: Interpreta o texto extraído e estrutura as informações em formato JSON (retornando null caso algum dado não exista, sem inventar informações).
5. Spring Boot (Validação): Desserializa o JSON em um record Java (DadosProcuracao) e aplica as regras de negócio (campos obrigatórios, dígitos de CPF/CNPJ, OAB ativa, assinatura, validade e conferência com o processo no PJe).
6. Persistência e Retorno: Salva o resultado no PostgreSQL e devolve o status imediatamente. Documentos aprovados seguem sem conferência manual; reprovados retornam o motivo em linguagem simples e podem ser encaminhados para revisão humana.

## Estrutura do Projeto
A aplicação Java está organizada em seis pacotes principais dentro de src/main/java/br/tjpe/procuracao/:

api/: Camada de entrada com os controllers REST responsáveis por receber os arquivos (ex: POST /documentos/validar) e devolver os resultados.

domain/: Modelos de domínio da aplicação, incluindo os records de dados extraídos (DadosProcuracao, Parte) e os enums de status de validação.

validation/: Motor de regras de negócio, responsável por validar formatos e dígitos verificadores de CPF/CNPJ, situação da OAB, prazos e consistência com o PJe.

ocr/: Integração com o OpenCV e o Tesseract para limpeza visual da imagem e extração de texto.

ai/: Cliente de comunicação com o modelo de linguagem (LLM) que converte o texto não estruturado em JSON.

persistence/: Entidades JPA e repositórios responsáveis por salvar os logs, dados extraídos e status no banco PostgreSQL.

## Escopo do MVP (Produto Mínimo Viável)

O MVP entrega o fluxo completo de ponta a ponta — da captura do documento pelo advogado até a resposta automática da validação — dividido em 4 pilares principais:

1. Interface do Usuário (Front-end Mobile-First)

Tela responsiva otimizada para celular e desktop.

Permite fotografar a procuração na hora ou anexar um arquivo (JPG, PNG ou PDF).

Exibe imediatamente o resultado da validação e os motivos detalhados em caso de pendência.

2. Processamento Inteligente (Pipeline de Dados)

Tratamento de Imagem (OpenCV): Corrige rotação, perspectiva, contraste e ruído.

Extração de Texto (Tesseract OCR): Converte a imagem tratada em texto bruto.

Estruturação com IA (LLM): Interpreta o texto e gera um JSON padronizado com os dados do documento.

3. Back-end e Regras de Negócio (Spring Boot + PostgreSQL)

API REST: Endpoint POST /documentos/validar para recebimento e análise dos arquivos.

Motor de Validação em Java: Verifica automaticamente campos obrigatórios, validade do CPF/CNPJ, situação da OAB, presença de assinatura e prazos.

Banco de Dados: Persistência dos dados extraídos, status final e justificativas no PostgreSQL.

4. Cenários de Demonstração (Prova de Conceito)
O sistema será validado com três casos práticos reais:

Cenário 1 — Aprovado: Procuração legível, assinada e com todos os dados válidos.

Cenário 2 — Rejeitado por Prazo: Documento legível, porém com validade expirada.

Cenário 3 — Rejeitado por Qualidade: Foto ilegível que impede a leitura segura mesmo após o tratamento de imagem.
