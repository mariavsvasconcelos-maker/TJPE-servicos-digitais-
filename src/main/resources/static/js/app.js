/**
 * TJPE — Validação Inteligente de Procurações Ad Judicia
 * CESAR School e Porto Digital — Projeto Acadêmico
 * app.js — liga a tela à API REST do Spring Boot.
 */

'use strict';

// Quando a página é servida pelo próprio Spring Boot, a API está no mesmo endereço.
const API_BASE = location.protocol.startsWith('http') ? '/api' : 'http://localhost:8080/api';

const els = {
  form:            document.getElementById('uploadForm'),
  uploadZone:      document.getElementById('uploadZone'),
  fileInput:       document.getElementById('fileInput'),
  arquivoNome:     document.getElementById('arquivoNome'),
  cnjInput:        document.getElementById('cnjInput'),
  btnProcessar:    document.getElementById('btnProcessar'),
  progresso:       document.getElementById('progressoSection'),
  progressBar:     document.getElementById('progressBar'),
  progressLabel:   document.getElementById('progressLabel'),
  etapas:          document.querySelectorAll('.etapa'),
  resultado:       document.getElementById('resultadoSection'),
  resultadoConteudo: document.getElementById('resultadoConteudo'),
  tbodyHistorico:  document.getElementById('tbodyHistorico'),
  btnAtualizarHist: document.getElementById('btnAtualizarHist'),
  toasts:          document.getElementById('toastContainer'),
  botoesCenario:   document.querySelectorAll('[data-cenario]'),
};

let processando = false;

document.addEventListener('DOMContentLoaded', () => {
  els.botoesCenario.forEach((btn) => btn.addEventListener('click', () => executarCenario(btn.dataset.cenario)));
  els.fileInput.addEventListener('change', () => selecionarArquivo(els.fileInput.files[0]));
  ['dragenter', 'dragover'].forEach((ev) => els.uploadZone.addEventListener(ev, () => els.uploadZone.classList.add('arrastando')));
  ['dragleave', 'drop'].forEach((ev) => els.uploadZone.addEventListener(ev, () => els.uploadZone.classList.remove('arrastando')));
  els.form.addEventListener('submit', (e) => { e.preventDefault(); enviarDocumento(); });
  els.btnAtualizarHist.addEventListener('click', carregarHistorico);
  carregarHistorico();
});

// ================================================================
//  ENVIO E CENÁRIOS
// ================================================================
function selecionarArquivo(arquivo) {
  if (!arquivo) {
    els.arquivoNome.classList.add('hidden');
    els.btnProcessar.disabled = true;
    return;
  }
  els.arquivoNome.textContent = `📄 ${arquivo.name} (${formatarTamanho(arquivo.size)})`;
  els.arquivoNome.classList.remove('hidden');
  els.btnProcessar.disabled = false;
}

async function enviarDocumento() {
  const arquivo = els.fileInput.files[0];
  if (!arquivo) {
    mostrarToast('Selecione o arquivo da procuração primeiro.', 'erro');
    return;
  }
  const formData = new FormData();
  formData.append('file', arquivo);
  const cnj = els.cnjInput.value.trim();
  if (cnj) formData.append('numeroProcessoCnj', cnj);

  await processar(() => fetch(`${API_BASE}/procuracoes/processar`, { method: 'POST', body: formData }));
}

async function executarCenario(id) {
  await processar(() => fetch(`${API_BASE}/testes/cenario/${encodeURIComponent(id)}`, { method: 'POST' }));
}

async function processar(chamada) {
  if (processando) return;
  processando = true;
  definirBloqueio(true);
  els.resultado.classList.add('hidden');
  els.progresso.classList.remove('hidden');
  els.progresso.scrollIntoView({ behavior: 'smooth', block: 'center' });

  const passos = [
    'Enviando documento…',
    'Tratando a imagem (tons de cinza, contraste, binarização)…',
    'Lendo o texto com OCR…',
    'Interpretando os dados com IA…',
    'Aplicando as 10 regras do TJPE…',
  ];
  let passo = 0;
  atualizarProgresso(passo, passos[passo]);
  const timer = setInterval(() => {
    if (passo < passos.length - 1) {
      passo++;
      atualizarProgresso(passo, passos[passo]);
    }
  }, 450);

  try {
    const [response] = await Promise.all([chamada(), esperar(1800)]);
    const corpo = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(corpo.message || corpo.error || `Erro HTTP ${response.status}`);
    }
    clearInterval(timer);
    atualizarProgresso(passos.length, 'Concluído!');
    await esperar(350);
    els.progresso.classList.add('hidden');
    renderizarResultado(corpo);
    carregarHistorico();
  } catch (err) {
    clearInterval(timer);
    els.progresso.classList.add('hidden');
    const semServidor = err instanceof TypeError;
    mostrarToast(semServidor
      ? 'Não foi possível falar com o servidor. Verifique se o sistema está rodando.'
      : `Erro: ${err.message}`, 'erro');
  } finally {
    processando = false;
    definirBloqueio(false);
  }
}

function definirBloqueio(bloquear) {
  els.botoesCenario.forEach((b) => { b.disabled = bloquear; });
  els.btnProcessar.disabled = bloquear || !els.fileInput.files[0];
}

function atualizarProgresso(indice, texto) {
  const total = els.etapas.length;
  els.etapas.forEach((el, i) => {
    el.classList.toggle('concluida', i < indice);
    el.classList.toggle('ativa', i === indice);
  });
  els.progressBar.style.width = `${Math.min(100, Math.round(((indice + 1) / (total + 1)) * 100))}%`;
  if (indice >= total) els.progressBar.style.width = '100%';
  els.progressLabel.textContent = texto;
}

// ================================================================
//  RESULTADO
// ================================================================
const STATUS = {
  APROVADO:       { cls: 'aprovado',  icone: '✅', titulo: 'Procuração aprovada',        motivos: 'Parecer' },
  REPROVADO:      { cls: 'reprovado', icone: '❌', titulo: 'Procuração reprovada',       motivos: 'Motivos da reprovação' },
  REVISAO_MANUAL: { cls: 'revisao',   icone: '⚠️', titulo: 'Encaminhada para revisão manual', motivos: 'Motivos do encaminhamento' },
};

function infoStatus(status) {
  return STATUS[status] || STATUS.REVISAO_MANUAL;
}

function renderizarResultado(dados) {
  const resultado = dados.resultadoValidacao || {};
  const info = infoStatus(resultado.status);
  const justificativas = resultado.justificativas || [];
  const regras = resultado.regrasDetalhadas || [];

  els.resultadoConteudo.innerHTML = `
    <div class="resultado-banner ${info.cls}">
      <span class="resultado-icone" aria-hidden="true">${info.icone}</span>
      <div>
        <div class="resultado-titulo">${info.titulo}</div>
        <div class="resultado-descricao">${escaparHtml(resultado.statusDescricao || '')}</div>
        <div class="resultado-meta">Arquivo: ${escaparHtml(dados.nomeArquivo || '—')} · Registro nº ${dados.id ?? '—'} · ${formatarDataHora(dados.dataUpload)}</div>
      </div>
    </div>

    ${justificativas.length ? `
      <h3 class="section-title">${info.motivos}</h3>
      <ul class="motivos">
        ${justificativas.map((j) => `<li class="motivo ${info.cls}">${escaparHtml(j)}</li>`).join('')}
      </ul>` : ''}

    ${regras.length ? `
      <h3 class="section-title">Regras verificadas</h3>
      <ul class="regras">
        ${regras.map(renderRegra).join('')}
      </ul>` : ''}

    <h3 class="section-title">Dados extraídos pela IA</h3>
    ${renderDadosExtraidos(dados.dadosExtraidos || {})}

    ${dados.textoExtraidoOcr ? `
      <details class="ocr">
        <summary>Ver o texto lido do documento (OCR)</summary>
        <pre>${escaparHtml(dados.textoExtraidoOcr)}</pre>
      </details>` : ''}
  `;

  els.resultado.classList.remove('hidden');
  els.resultado.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function classificarRegra(texto) {
  const t = texto.toUpperCase();
  const veredito = t.includes(':') ? t.slice(t.indexOf(':') + 1) : t;
  if (veredito.includes('REPROVADO') || veredito.includes('FALHA')) return { cls: 'erro', icone: '✕' };
  if (veredito.includes('REVISÃO') || veredito.includes('ALERTA')) return { cls: 'aviso', icone: '!' };
  if (veredito.includes('INFORMATIVO')) return { cls: 'info', icone: 'i' };
  return { cls: 'ok', icone: '✓' };
}

function renderRegra(texto) {
  const c = classificarRegra(texto);
  return `<li class="regra ${c.cls}"><span class="regra-icone" aria-hidden="true">${c.icone}</span><span>${escaparHtml(texto)}</span></li>`;
}

function renderDadosExtraidos(d) {
  const assinatura = {
    DIGITAL: 'Digital (Gov.br / ICP-Brasil)', FISICA: 'Física (manuscrita)',
    AUSENTE: 'Ausente', ILEGIVEL: 'Ilegível',
  };
  const campos = [
    ['Nº do processo (CNJ)', d.numeroProcessoCnj],
    ['Outorgante', d.outorganteNome],
    ['CPF/CNPJ do outorgante', d.outorganteCpfCnpj],
    ['Outorgado (advogado)', d.outorgadoNome],
    ['CPF do advogado', d.outorgadoCpfCnpj],
    ['Inscrição na OAB', d.outorgadoOabNumero],
    ['Seccional da OAB', d.outorgadoOabSeccional],
    ['Data de outorga', formatarData(d.dataOutorga)],
    ['Prazo de validade', d.prazoValidade ? formatarData(d.prazoValidade) : 'Indeterminado (sem data final)'],
    ['Poderes concedidos', d.poderesConcedidos],
    ['Finalidade', d.finalidade],
    ['Assinatura', assinatura[d.tipoAssinatura] || (d.temAssinatura ? 'Identificada' : null)],
    ['Carimbos e selos', (d.carimbosSelosPresentes || []).join(' · ') || null],
  ];
  return `<div class="dados-grid">${campos.map(([label, valor]) => {
    const vazio = valor === null || valor === undefined || valor === '';
    return `<div class="dado-item">
      <div class="dado-label">${label}</div>
      <div class="dado-valor ${vazio ? 'nulo' : ''}">${vazio ? 'Não identificado' : escaparHtml(String(valor))}</div>
    </div>`;
  }).join('')}</div>`;
}

// ================================================================
//  HISTÓRICO
// ================================================================
async function carregarHistorico() {
  try {
    const response = await fetch(`${API_BASE}/procuracoes`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    renderHistorico(await response.json());
  } catch (err) {
    els.tbodyHistorico.innerHTML = '<tr><td colspan="5" class="vazio">Não foi possível carregar o histórico. O servidor está rodando?</td></tr>';
  }
}

function renderHistorico(lista) {
  if (!lista.length) {
    els.tbodyHistorico.innerHTML = '<tr><td colspan="5" class="vazio">Nenhuma procuração validada ainda. Rode um cenário acima!</td></tr>';
    return;
  }
  els.tbodyHistorico.innerHTML = lista.map((item) => {
    const info = infoStatus(item.status);
    const rotulo = { APROVADO: 'Aprovado', REPROVADO: 'Reprovado', REVISAO_MANUAL: 'Revisão manual' }[item.status] || '—';
    return `<tr>
      <td>📄 ${escaparHtml(item.nomeArquivo || `#${item.id}`)}</td>
      <td><span class="status-badge ${info.cls}">${rotulo}</span></td>
      <td>${formatarDataHora(item.dataUpload)}</td>
      <td>${escaparHtml(item.numeroProcessoCnj || '—')}</td>
      <td><button type="button" class="btn-outline" data-id="${item.id}">Ver</button></td>
    </tr>`;
  }).join('');
  els.tbodyHistorico.querySelectorAll('[data-id]').forEach((btn) =>
    btn.addEventListener('click', () => verDetalhe(btn.dataset.id)));
}

async function verDetalhe(id) {
  try {
    const response = await fetch(`${API_BASE}/procuracoes/${encodeURIComponent(id)}`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    renderizarResultado(await response.json());
  } catch (err) {
    mostrarToast(`Erro ao abrir o registro: ${err.message}`, 'erro');
  }
}

// ================================================================
//  UTILITÁRIOS
// ================================================================
function esperar(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function formatarData(iso) {
  if (!iso) return null;
  const [ano, mes, dia] = String(iso).slice(0, 10).split('-');
  return dia && mes && ano ? `${dia}/${mes}/${ano}` : String(iso);
}

function formatarDataHora(iso) {
  if (!iso) return '—';
  const data = new Date(iso);
  return isNaN(data) ? String(iso) : data.toLocaleString('pt-BR');
}

function formatarTamanho(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function escaparHtml(texto) {
  return String(texto)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function mostrarToast(mensagem, tipo = 'info') {
  const toast = document.createElement('div');
  toast.className = `toast ${tipo}`;
  toast.textContent = mensagem;
  els.toasts.appendChild(toast);
  setTimeout(() => toast.remove(), 5000);
}
