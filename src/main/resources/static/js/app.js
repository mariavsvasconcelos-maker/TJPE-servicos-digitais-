document.addEventListener('DOMContentLoaded', () => {
    const uploadForm = document.getElementById('uploadForm');
    const fileInput = document.getElementById('fileInput');
    const dropZone = document.getElementById('dropZone');
    const selectedFileInfo = document.getElementById('selectedFileInfo');
    const fileNameSpan = document.getElementById('fileName');
    const removeFileBtn = document.getElementById('removeFileBtn');
    const btnSubmit = document.getElementById('btnSubmit');
    const pipelineSection = document.getElementById('pipelineSection');
    const resultSection = document.getElementById('resultSection');

    // Inicializa histórico
    carregarHistorico();

    // Eventos de Seleção de Arquivo
    fileInput.addEventListener('change', (e) => {
        if (fileInput.files.length > 0) {
            handleFileSelected(fileInput.files[0]);
        }
    });

    // Drag and Drop
    ['dragenter', 'dragover'].forEach(eventName => {
        dropZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropZone.classList.add('dragover');
        });
    });

    ['dragleave', 'drop'].forEach(eventName => {
        dropZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropZone.classList.remove('dragover');
        });
    });

    dropZone.addEventListener('drop', (e) => {
        const dt = e.dataTransfer;
        const files = dt.files;
        if (files.length > 0) {
            fileInput.files = files;
            handleFileSelected(files[0]);
        }
    });

    removeFileBtn.addEventListener('click', () => {
        fileInput.value = '';
        selectedFileInfo.classList.add('hidden');
        dropZone.classList.remove('hidden');
        btnSubmit.disabled = true;
    });

    function handleFileSelected(file) {
        fileNameSpan.textContent = `Arquivo selecionado: ${file.name} (${(file.size / 1024).toFixed(1)} KB)`;
        selectedFileInfo.classList.remove('hidden');
        dropZone.classList.add('hidden');
        btnSubmit.disabled = false;
    }

    // Envio do Formulário de Upload
    uploadForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!fileInput.files.length) return;

        const formData = new FormData();
        formData.append('file', fileInput.files[0]);
        const cnj = document.getElementById('numeroProcessoCnj').value.trim();
        if (cnj) {
            formData.append('numeroProcessoCnj', cnj);
        }

        await processarRequisicao('/api/procuracoes/processar', {
            method: 'POST',
            body: formData
        });
    });
});

// Disparo de Cenários Obrigatórios de Teste
async function executarCenario(cenarioId) {
    await processarRequisicao(`/api/testes/cenario/${cenarioId}`, {
        method: 'POST'
    });
}

// Fluxo de Processamento Genérico
async function processarRequisicao(url, options) {
    const pipelineSection = document.getElementById('pipelineSection');
    const resultSection = document.getElementById('resultSection');
    const btnSubmit = document.getElementById('btnSubmit');

    try {
        if (btnSubmit) btnSubmit.disabled = true;
        pipelineSection.classList.remove('hidden');
        resultSection.classList.add('hidden');
        pipelineSection.scrollIntoView({ behavior: 'smooth' });

        const response = await fetch(url, options);
        if (!response.ok) {
            const err = await response.json().catch(() => ({ message: 'Erro na comunicação com o servidor.' }));
            throw new Error(err.message || 'Erro ao processar procuração.');
        }

        const data = await response.json();
        renderizarResultado(data);
        carregarHistorico();

    } catch (err) {
        alert('Falha na validação: ' + err.message);
    } finally {
        pipelineSection.classList.add('hidden');
        if (btnSubmit && document.getElementById('fileInput').files.length > 0) {
            btnSubmit.disabled = false;
        }
    }
}

// Renderização dos Resultados na Interface
function renderizarResultado(data) {
    const resultSection = document.getElementById('resultSection');
    const badge = document.getElementById('resultStatusBadge');
    const title = document.getElementById('resultTitle');
    const subtitle = document.getElementById('resultSubtitle');
    const justList = document.getElementById('justificationsList');
    const extractedBody = document.getElementById('extractedDataBody');
    const rulesList = document.getElementById('rulesDetailList');
    const rawOcr = document.getElementById('rawOcrText');

    const validacao = data.resultadoValidacao;
    const status = validacao.status;

    // Badge e Cores
    badge.className = 'status-badge';
    if (status === 'APROVADO') {
        badge.classList.add('badge-aprovado');
        badge.textContent = 'APROVADO';
        title.textContent = 'Procuração Validada com Sucesso';
    } else if (status === 'REPROVADO') {
        badge.classList.add('badge-reprovado');
        badge.textContent = 'REPROVADO';
        title.textContent = 'Procuração Não Conforme - Rejeitada';
    } else {
        badge.classList.add('badge-revisao');
        badge.textContent = 'REVISÃO MANUAL';
        title.textContent = 'Encaminhado para Revisão de Servidor';
    }

    subtitle.textContent = `Documento: ${data.nomeArquivo} | Hash SHA-256: ${data.hashSha256.substring(0, 16)}... | Processado em: ${new Date(validacao.dataValidacao).toLocaleString('pt-BR')}`;

    // Justificativas
    justList.innerHTML = '';
    if (validacao.justificativas && validacao.justificativas.length > 0) {
        validacao.justificativas.forEach(j => {
            const li = document.createElement('li');
            li.textContent = j;
            justList.appendChild(li);
        });
    }

    // Tabela com os 11 Campos Extraídos
    extractedBody.innerHTML = '';
    const d = data.dadosExtraidos;
    const campos = [
        { label: 'Número do Processo (CNJ)', valor: d.numeroProcessoCnj },
        { label: 'Nome do Outorgante', valor: d.outorganteNome },
        { label: 'CPF / CNPJ do Outorgante', valor: d.outorganteCpfCnpj },
        { label: 'Nome do Outorgado (Advogado)', valor: d.outorgadoNome },
        { label: 'CPF do Outorgado', valor: d.outorgadoCpfCnpj },
        { label: 'Inscrição na OAB', valor: d.outorgadoOabNumero ? `${d.outorgadoOabNumero}/${d.outorgadoOabSeccional || 'PE'}` : null },
        { label: 'Data de Outorga', valor: d.dataOutorga ? formatarData(d.dataOutorga) : null },
        { label: 'Prazo de Validade', valor: d.prazoValidade ? formatarData(d.prazoValidade) : 'Prazo Indeterminado (Art. 682 CC)' },
        { label: 'Poderes Concedidos', valor: d.poderesConcedidos },
        { label: 'Finalidade da Procuração', valor: d.finalidade },
        { label: 'Assinatura', valor: d.tipoAssinatura ? `${d.tipoAssinatura} (${d.temAssinatura ? 'Presente' : 'Ausente'})` : null },
        { label: 'Carimbos e Selos', valor: d.carimbosSelosPresentes && d.carimbosSelosPresentes.length ? d.carimbosSelosPresentes.join(', ') : null }
    ];

    campos.forEach(c => {
        const tr = document.createElement('tr');
        const isNull = c.valor === null || c.valor === undefined;
        tr.innerHTML = `
            <td><strong>${c.label}</strong></td>
            <td>${isNull ? '<span class="text-muted"><em>null (não identificado no documento)</em></span>' : escapeHtml(c.valor)}</td>
            <td>${isNull ? '<span class="scenario-tag tag-reprovado">Ausente</span>' : '<span class="scenario-tag tag-aprovado">Identificado</span>'}</td>
        `;
        extractedBody.appendChild(tr);
    });

    // Detalhamento das 10 Regras
    rulesList.innerHTML = '';
    if (validacao.regrasDetalhadas && validacao.regrasDetalhadas.length > 0) {
        validacao.regrasDetalhadas.forEach(r => {
            const li = document.createElement('li');
            li.textContent = r;
            rulesList.appendChild(li);
        });
    }

    // Texto OCR Bruto
    rawOcr.textContent = data.textoExtraidoOcr || 'Nenhum texto detectado.';

    // Exibe seção de resultado
    resultSection.classList.remove('hidden');
    resultSection.scrollIntoView({ behavior: 'smooth' });
}

// Carregar Histórico
async function carregarHistorico() {
    const tbody = document.getElementById('historyTableBody');
    try {
        const res = await fetch('/api/procuracoes');
        if (!res.ok) return;
        const lista = await res.json();

        if (lista.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">Nenhuma procuração processada até o momento.</td></tr>';
            return;
        }

        tbody.innerHTML = '';
        lista.forEach(item => {
            const tr = document.createElement('tr');
            let badgeClass = 'tag-aprovado';
            if (item.status === 'REPROVADO') badgeClass = 'tag-reprovado';
            if (item.status === 'REVISAO_MANUAL') badgeClass = 'tag-revisao';

            tr.innerHTML = `
                <td>#${item.id}</td>
                <td><strong>${escapeHtml(item.nomeArquivo)}</strong></td>
                <td>${item.numeroProcessoCnj ? escapeHtml(item.numeroProcessoCnj) : '<span class="text-muted">-</span>'}</td>
                <td>${item.outorganteNome ? escapeHtml(item.outorganteNome) : '<span class="text-muted">Não identificado</span>'}</td>
                <td>${item.outorgadoNome ? escapeHtml(item.outorgadoNome) : '<span class="text-muted">Não identificado</span>'}</td>
                <td>${new Date(item.dataUpload).toLocaleString('pt-BR')}</td>
                <td><span class="scenario-tag ${badgeClass}">${item.status}</span></td>
                <td><button onclick="visualizarPorId(${item.id})" class="btn btn-sm btn-outline">Ver Detalhes</button></td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        console.error('Erro ao carregar histórico:', e);
    }
}

async function visualizarPorId(id) {
    try {
        const res = await fetch(`/api/procuracoes/${id}`);
        if (!res.ok) throw new Error('Não foi possível carregar os detalhes.');
        const data = await res.json();
        renderizarResultado(data);
    } catch (e) {
        alert(e.message);
    }
}

function formatarData(dataIso) {
    if (!dataIso) return '-';
    const partes = dataIso.split('-');
    if (partes.length === 3) {
        return `${partes[2]}/${partes[1]}/${partes[0]}`;
    }
    return dataIso;
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
