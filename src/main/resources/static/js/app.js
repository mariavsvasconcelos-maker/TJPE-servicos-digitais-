/**
 * TJPE — Validador Inteligente de Procurações Ad Judicia
 * CESAR School — Projeto Acadêmico
 * app.js — Lógica principal do front-end
 */

'use strict';

// ================================================================
//  CONSTANTES
// ================================================================
const API_BASE = 'http://localhost:8080/api';

const ETAPAS = [
  { id: 'upload',    label: 'Upload',      icon: '📤' },
  { id: 'imagem',    label: 'Imagem',       icon: '🖼️' },
  { id: 'ocr',       label: 'OCR',          icon: '🔍' },
  { id: 'ia',        label: 'IA',           icon: '🤖' },
  { id: 'validacao', label: 'Validação',    icon: '✅' },
];

// ================================================================
//  ESTADO GLOBAL
// ================================================================
const state = {
  arquivoSelecionado: null,
  processando: false,
};

// ================================================================
//  REFERÊNCIAS DOM
// ================================================================
const els = {
  uploadZone:         document.getElementById('uploadZone'),
  fileInput:          document.getElementById('fileInput'),
  arquivoPreview:     document.getElementById('arquivoPreview'),
  arquivoNome:        document.getElementById('arquivoNome'),
  arquivoTamanho:     document.getElementById('arquivoTamanho'),
  cnjInput:           document.getElementById('cnjInput'),
  btnProcessar:       document.getElementById('btnProcessar'),
  progressContainer:  document.getElementById('progressContainer'),
  progressBar:        document.getElementById('progressBar'),
  progressLabel:      document.getElementById('progressLabel'),
  progressPct:        document.getElementById('progressPct'),
  stepItems:          document.querySelectorAll('.step-item'),
  resultadoSection:   document.getElementById('resultadoSection'),
  resultadoConteudo:  document.getElementById('resultadoConteudo'),
  historicoSection:   document.getElementById('historicoSection'),
  tbodyHistorico:     document.getElementById('tbodyHistorico'),
  btnAtualizarHist:   document.getElementById('btnAtualizarHist'),
  toastContainer:     document.getElementById('toastContainer'),
};

// ================================================================
//  INICIALIZAÇÃO
// ================================================================
document.addEventListener('DOMContentLoaded', () => {
  inicializarUpload();
  inicializarCenarios();
  carregarHistorico();
});

// ================================================================
//  UPLOAD — DRAG & DROP + FILE INPUT
// ================================================================
function inicializarUpload() {
  const zone = els.uploadZone;
  const input = els.fileInput;

  // Clique na zona de upload
  zone.addEventListener('click', (e) => {
    if (e.target !== input) input.click();
  });

  // Drag events
  zone.addEventListener('dragover', (e) => {
    e.preventDefault();
    zone.classList.add('drag-over');
  });

  zone.addEventListener('dragleave', () => zone.classList.remove('drag-over'));

  zone.addEventListener('drop', (e) => {
    e.preventDefault();
    zone.classList.remove('drag-over');
    const arquivo = e.dataTransfer.files[0];
    if (arquivo) selecionarArquivo(arquivo);
  });

  // Input file change
  input.addEventListener('change', () => {
    if (input.files[0]) selecionarArquivo(input.files[0]);
  });

  // Botão processar
  els.btnProcessar.addEventListener('click', processarDocumento);

  // Atualizar histórico
  els.btnAtualizarHist?.addEventListener('click', carregarHistorico);
}

function selecionarArquivo(arquivo) {
  const extensoesPermitidas = ['pdf', 'jpg', 'jpeg', 'png'];
  const ext = arquivo.name.split('.').pop().toLowerCase();

  if (!extensoesPermitidas.includes(ext)) {
    mostrarToast('Formato não suportado. Use PDF, JPG ou PNG.', 'erro');
    return;
  }

  if (arquivo.size > 20 * 1024 * 1024) {
    mostrarToast('Arquivo muito grande. Tamanho máximo: 20 MB.', 'erro');
    return;
  }

  state.arquivoSelecionado = arquivo;
  els.arquivoPreview.classList.remove('hidden');
  els.arquivoNome.textContent = arquivo.name;
  els.arquivoTamanho.textContent = formatarTamanho(arquivo.size);
  els.btnProcessar.disabled = false;

  mostrarToast(`Arquivo "${arquivo.name}" selecionado.`, 'info');
}

// ================================================================
//  PROCESSAMENTO DO DOCUMENTO
// ================================================================
async function processarDocumento() {
  if (!state.arquivoSelecionado || state.processando) return;

  state.processando = true;
  els.btnProcessar.disabled = true;

  const formData = new FormData();
  formData.append('arquivo', state.arquivoSelecionado);

  const cnj = els.cnjInput.value.trim();
  if (cnj) formData.append('numeroProcessoCnj', cnj);

  mostrarProgresso(true);
  atualizarEtapa(0);

  try {
    // Simula progresso por etapas enquanto aguarda a API
    const intervalos = [
      { pct: 20, etapa: 1, label: 'Pré-processando imagem com OpenCV…' },
      { pct: 45, etapa: 2, label: 'Extraindo texto com Tesseract OCR…' },
      { pct: 70, etapa: 3, label: 'Interpretando dados com IA…' },
      { pct: 90, etapa: 4, label: 'Aplicando regras de validação…' },
    ];

    let idx = 0;
    const timer = setInterval(() => {
      if (idx < intervalos.length) {
        const { pct, etapa, label } = intervalos[idx];
        setProgressBar(pct, label);
        atualizarEtapa(etapa);
        idx++;
      }
    }, 900);

    const response = await fetch(`${API_BASE}/procuracoes/processar`, {
      method: 'POST',
      body: formData,
    });

    clearInterval(timer);

    if (!response.ok) {
      const erro = await response.json().catch(() => ({ message: 'Erro desconhecido.' }));
      throw new Error(erro.message || `HTTP ${response.status}`);
    }

    const dados = await response.json();

    setProgressBar(100, 'Concluído!');
    atualizarEtapa(4, true);

    setTimeout(() => {
      mostrarProgresso(false);
      renderizarResultado(dados);
      carregarHistorico();
    }, 600);

  } catch (err) {
    mostrarProgresso(false);
    console.error('Erro ao processar documento:', err);
    mostrarToast(`Erro: ${err.message}`, 'erro');
  } finally {
    state.processando = false;
    els.btnProcessar.disabled = false;
  }
}

// ================================================================
//  CENÁRIOS DE TESTE
// ================================================================
function inicializarCenarios() {
  document.querySelectorAll('[data-cenario]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const id = btn.dataset.cenario;
      executarCenario(id);
    });
  });
}

async function executarCenario(id) {
  mostrarToast(`Executando Cenário ${id}…`, 'info');
  mostrarProgresso(true);
  atualizarEtapa(0);

  try {
    const intervalos = [
      { pct: 25, etapa: 1 },
      { pct: 50, etapa: 2 },
      { pct: 75, etapa: 3 },
      { pct: 90, etapa: 4 },
    ];

    let idx = 0;
    const timer = setInterval(() => {
      if (idx < intervalos.length) {
        const { pct, etapa } = intervalos[idx];
        setProgressBar(pct, `Processando etapa ${etapa + 1}…`);
        atualizarEtapa(etapa);
        idx++;
      }
    }, 700);

    const response = await fetch(`${API_BASE}/testes/cenario/${id}`, { method: 'POST' });
    clearInterval(timer);

    if (!response.ok) throw new Error(`HTTP ${response.status}`);

    const dados = await response.json();
    setProgressBar(100, 'Concluído!');
    atualizarEtapa(4, true);

    setTimeout(() => {
      mostrarProgresso(false);
      renderizarResultado(dados);
      carregarHistorico();
    }, 600);

  } catch (err) {
    mostrarProgresso(false);
    mostrarToast(`Erro no cenário: ${err.message}`, 'erro');
  }
}

// ================================================================
//  RENDERIZAÇÃO DO RESULTADO
// ================================================================
function renderizarResultado(dados) {
  const section = els.resultadoSection;
  const container = els.resultadoConteudo;

  section.classList.remove('hidden');
  section.scrollIntoView({ behavior: 'smooth', block: 'start' });

  const resultado = dados.resultado || dados;
  const extraidos = dados.dadosExtraidos || {};
  const checks    = resultado.checks || resultado.verificacoes || [];
  const motivos   = resultado.motivosRejeicao || resultado.justificativas || [];
  const status    = (resultado.status || resultado.statusValidacao || 'REVISAO_MANUAL').toUpperCase();

  const statusInfo = resolverStatus(status);

  container.innerHTML = `
    <div class="resultado-header ${statusInfo.cls}">
      <span class="resultado-icone">${statusInfo.icon}</span>
      <div>
        <div class="resultado-titulo ${statusInfo.cls}">${statusInfo.label}</div>
        <div class="resultado-subtitulo">
          ${resultado.mensagem || 'Validação concluída pelo sistema.'}
          ${resultado.id ? `· <small>ID #${resultado.id}</small>` : ''}
        </div>
      </div>
    </div>

    <!-- Dados Extraídos -->
    ${renderDadosExtraidos(extraidos)}

    <!-- Checks -->
    ${renderChecks(checks)}

    <!-- Motivos de Rejeição -->
    ${motivos.length ? renderMotivos(motivos, status) : ''}
  `;
}

function resolverStatus(status) {
  if (status === 'APROVADO')
    return { cls: 'aprovado',  icon: '✅', label: 'PROCURAÇÃO APROVADA' };
  if (status === 'REPROVADO')
    return { cls: 'reprovado', icon: '❌', label: 'PROCURAÇÃO REPROVADA' };
  return { cls: 'revisao', icon: '⚠️', label: 'REVISÃO MANUAL NECESSÁRIA' };
}

function renderDadosExtraidos(dados) {
  const campos = [
    { key: 'numeroProcessoCnj',   label: 'Nº do Processo (CNJ)' },
    { key: 'nomeOutorgante',       label: 'Outorgante' },
    { key: 'cpfCnpjOutorgante',    label: 'CPF/CNPJ Outorgante' },
    { key: 'nomeOutorgado',        label: 'Outorgado (Advogado)' },
    { key: 'cpfCnpjOutorgado',     label: 'CPF/CNPJ Outorgado' },
    { key: 'numeroOab',            label: 'Nº OAB' },
    { key: 'secionalOab',          label: 'Seccional OAB' },
    { key: 'dataOutorga',          label: 'Data de Outorga' },
    { key: 'dataValidade',         label: 'Validade' },
    { key: 'poderesConcedidos',    label: 'Poderes Concedidos' },
    { key: 'finalidade',           label: 'Finalidade' },
    { key: 'possuiAssinatura',     label: 'Assinatura' },
    { key: 'tipoAssinatura',       label: 'Tipo de Assinatura' },
    { key: 'possuiCarimbo',        label: 'Carimbo / Selo' },
  ];

  if (Object.keys(dados).length === 0) return '';

  const items = campos.map(({ key, label }) => {
    let valor = dados[key];
    let nulo = false;

    if (valor === null || valor === undefined || valor === '') {
      valor = 'Não identificado';
      nulo = true;
    } else if (typeof valor === 'boolean') {
      valor = valor ? 'Sim' : 'Não';
    }

    return `<div class="dado-item">
      <div class="dado-label">${label}</div>
      <div class="dado-valor ${nulo ? 'nulo' : ''}">${escaparHtml(String(valor))}</div>
    </div>`;
  }).join('');

  return `<div class="card-title" style="margin-top:0.5rem">📄 Dados Extraídos</div>
    <div class="dados-grid">${items}</div>`;
}

function renderChecks(checks) {
  if (!checks.length) return '';

  const items = checks.map((c) => {
    const tipo  = (c.tipo  || c.resultado || 'info').toLowerCase();
    const msg   = c.mensagem || c.descricao || String(c);
    const icons = { ok: '✅', erro: '❌', aviso: '⚠️', info: 'ℹ️' };
    return `<div class="check-item ${tipo}">
      <span class="check-icon">${icons[tipo] || 'ℹ️'}</span>
      <span>${escaparHtml(msg)}</span>
    </div>`;
  }).join('');

  return `<div class="card-title" style="margin-top:1.25rem">🔎 Verificações Realizadas</div>
    <div class="checks-lista">${items}</div>`;
}

function renderMotivos(motivos, status) {
  const cls = status === 'REPROVADO' ? '' : 'aviso';
  const items = motivos.map((m) =>
    `<div class="justificativa-item ${cls}">
      <span>${cls ? '⚠️' : '❌'}</span>
      <span>${escaparHtml(typeof m === 'string' ? m : m.mensagem || JSON.stringify(m))}</span>
    </div>`
  ).join('');

  return `<div class="card-title" style="margin-top:1.25rem">🚫 Motivos</div>
    <div class="justificativas-lista">${items}</div>`;
}

// ================================================================
//  HISTÓRICO
// ================================================================
async function carregarHistorico() {
  try {
    const response = await fetch(`${API_BASE}/procuracoes`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);

    const lista = await response.json();
    renderizarHistorico(Array.isArray(lista) ? lista : (lista.content || []));
  } catch (err) {
    console.warn('Histórico indisponível:', err.message);
  }
}

function renderizarHistorico(lista) {
  const tbody = els.tbodyHistorico;
  if (!tbody) return;

  if (!lista.length) {
    tbody.innerHTML = `
      <tr>
        <td colspan="5">
          <div class="historico-vazio">
            <div class="historico-vazio-icon">📂</div>
            Nenhuma procuração validada ainda.
          </div>
        </td>
      </tr>`;
    return;
  }

  tbody.innerHTML = lista.map((item) => {
    const status = (item.statusValidacao || item.status || '').toUpperCase();
    const info = resolverStatus(status);
    const badge = `<span class="status-badge ${info.cls}">${info.icon} ${status.replace('_', ' ')}</span>`;
    const data = item.dataProcessamento
      ? new Date(item.dataProcessamento).toLocaleString('pt-BR')
      : '—';

    return `<tr>
      <td class="td-arquivo" title="${escaparHtml(item.nomeArquivo || '')}">
        📄 ${escaparHtml(item.nomeArquivo || `#${item.id}`)}
      </td>
      <td>${badge}</td>
      <td class="td-data">${data}</td>
      <td>${escaparHtml(item.numeroProcessoCnj || '—')}</td>
      <td>
        <button class="btn btn-outline" style="padding:0.3rem 0.7rem;font-size:0.75rem"
          onclick="verDetalhe(${item.id})">
          Ver
        </button>
      </td>
    </tr>`;
  }).join('');
}

async function verDetalhe(id) {
  try {
    const response = await fetch(`${API_BASE}/procuracoes/${id}`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const dados = await response.json();
    renderizarResultado(dados);
    els.resultadoSection.scrollIntoView({ behavior: 'smooth' });
  } catch (err) {
    mostrarToast(`Erro ao carregar detalhe: ${err.message}`, 'erro');
  }
}

// ================================================================
//  PROGRESSO
// ================================================================
function mostrarProgresso(visivel) {
  if (visivel) {
    els.progressContainer.classList.remove('hidden');
    setProgressBar(5, 'Enviando documento…');
    resetarEtapas();
  } else {
    setTimeout(() => {
      els.progressContainer.classList.add('hidden');
      resetarEtapas();
    }, 1000);
  }
}

function setProgressBar(pct, label) {
  els.progressBar.style.width = `${pct}%`;
  els.progressLabel.textContent = label;
  els.progressPct.textContent = `${pct}%`;
}

function atualizarEtapa(indice, finalizar = false) {
  els.stepItems.forEach((el, i) => {
    el.classList.remove('ativo', 'concluido');
    if (i < indice) el.classList.add('concluido');
    else if (i === indice) el.classList.add(finalizar ? 'concluido' : 'ativo');
  });
}

function resetarEtapas() {
  els.stepItems.forEach((el) => el.classList.remove('ativo', 'concluido'));
}

// ================================================================
//  TOAST
// ================================================================
function mostrarToast(msg, tipo = 'info') {
  const icons = { sucesso: '✅', erro: '❌', info: 'ℹ️' };
  const toast = document.createElement('div');
  toast.className = `toast ${tipo}`;
  toast.innerHTML = `<span>${icons[tipo] || 'ℹ️'}</span><span>${escaparHtml(msg)}</span>`;
  els.toastContainer.appendChild(toast);
  setTimeout(() => toast.remove(), 4000);
}

// ================================================================
//  UTILITÁRIOS
// ================================================================
function formatarTamanho(bytes) {
  if (bytes < 1024)        return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function escaparHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
