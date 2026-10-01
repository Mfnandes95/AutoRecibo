/* AutoRecibo — cliente da API. Contrato ASSUMIDO: confira com os DTOs do backend. */
const CFG = {
  campoLogin: 'login',  // nome do campo no DTO de login (email, CPF ou CNPJ)
  campoToken: 'token',  // campo do JWT na resposta de /api/auth/login
  campoArquivo: 'file', // @RequestParam do upload em /api/catalogo/importar
};

const MSG = {
  400: 'Confira os dados informados.',
  401: 'Login ou senha incorretos.',
  403: 'Login ou senha incorretos.',
  409: 'Email ou documento já cadastrado.',
  413: 'Arquivo grande demais.',
};

const $ = (id) => document.getElementById(id);
const soDigitos = (s) => String(s).replace(/\D/g, '');
const brl = (v) => Number(v).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

// sessionStorage: o token some ao fechar a aba.
const Sessao = {
  get token() { return sessionStorage.getItem('jwt'); },
  salvar(t) { sessionStorage.setItem('jwt', t); },
  sair() { sessionStorage.removeItem('jwt'); location.href = 'index.html'; },
  exigir() { if (!this.token) location.replace('index.html'); },
  email() {
    try {
      const p = this.token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(atob(p)).sub || '';
    } catch { return ''; }
  },
};

async function api(caminho, { metodo = 'GET', corpo, form } = {}) {
  const headers = {};
  let body;
  if (Sessao.token) headers.Authorization = 'Bearer ' + Sessao.token;
  if (form) body = form;
  else if (corpo) { headers['Content-Type'] = 'application/json'; body = JSON.stringify(corpo); }

  let r;
  try { r = await fetch(caminho, { method: metodo, headers, body }); }
  catch { throw new Error('Sem conexão com o servidor. Tente novamente.'); }

  if (r.status === 401 && Sessao.token) { Sessao.sair(); throw new Error('Sessão expirada.'); }
  const dados = await r.json().catch(() => null);
  if (!r.ok) {
    const e = new Error(dados?.message || dados?.erro || MSG[r.status] || 'Algo deu errado. Tente de novo.');
    e.status = r.status;
    throw e;
  }
  return dados;
}

// Mostra erro/sucesso em um elemento com role="alert".
function aviso(el, texto, ok = false) {
  el.textContent = texto;
  el.className = 'text-sm rounded-xl px-4 py-3 border ' +
    (ok ? 'bg-emerald-50 text-emerald-700 border-emerald-100' : 'bg-red-50 text-red-700 border-red-100');
  el.hidden = !texto;
}

// Envia um formulário: desabilita o botão durante a chamada.
async function enviar(form, botao, erroEl, fn) {
  const rotulo = botao.textContent;
  botao.disabled = true; botao.textContent = 'Aguarde...'; erroEl.hidden = true;
  try { await fn(); }
  catch (e) { aviso(erroEl, e.message); }
  finally { botao.disabled = false; botao.textContent = rotulo; }
}
