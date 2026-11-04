import { useCallback, useEffect, useState } from 'react'
import { qa } from '../../api.js'
import { useAuth } from '../../auth.jsx'
import Icone from '../../components/Icone.jsx'
import { CabecalhoQa, ErroCarga, Esqueleto, dataHoraCurta } from '../../components/qa/QaComum.jsx'

const CAMPOS = [
  { chave: 'timeoutRequisicaoSegundos', rotulo: 'Tempo limite por requisição', unidade: 's', max: 'timeoutRequisicaoMax',
    ajuda: 'Quanto o executor espera cada chamada à API antes de marcar erro. Pode ser ajustado em cada execução.' },
  { chave: 'timeoutExecucaoMinutos', rotulo: 'Tempo limite da execução', unidade: 'min', max: 'timeoutExecucaoMax',
    ajuda: 'Passado esse tempo, a execução é encerrada com erro e o que não terminou é fechado.' },
  { chave: 'cenariosParalelos', rotulo: 'Cenários em paralelo', unidade: 'por execução', max: 'cenariosParalelosMax',
    ajuda: 'Quantos cenários da mesma execução rodam ao mesmo tempo.' },
  { chave: 'execucoesPorUsuario', rotulo: 'Execuções ativas por usuário', unidade: 'ao mesmo tempo', max: 'execucoesPorUsuarioMax',
    ajuda: 'Evita que uma pessoa ocupe todas as vagas do executor.' },
]

const ACOES = {
  EXECUCAO_CRIADA: 'Iniciou execução',
  EXECUCAO_REEXECUTADA: 'Reexecutou',
  EXECUCAO_CANCELADA: 'Cancelou execução',
  CONFIGURACAO_ALTERADA: 'Alterou configurações',
  SUITE_CRIADA: 'Criou Test Suite',
  SUITE_ALTERADA: 'Alterou Test Suite',
  SUITE_EXCLUIDA: 'Excluiu Test Suite',
}

const PERMISSOES = [
  ['qa.view', 'Ver catálogo, execuções e detalhes'],
  ['qa.execute', 'Iniciar e reexecutar testes'],
  ['qa.cancel', 'Cancelar execuções'],
  ['qa.reports.view', 'Ver relatórios e exportar'],
  ['qa.configure', 'Alterar configurações e Test Suites'],
]

// Configurações do executor + permissões do usuário + políticas de segurança + auditoria
export default function QaConfiguracoes() {
  const { pode } = useAuth()
  const podeConfigurar = pode('qa.configure')
  const [config, setConfig] = useState(null)
  const [valores, setValores] = useState({})
  const [auditoria, setAuditoria] = useState(null)
  const [erro, setErro] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [mensagem, setMensagem] = useState('')
  const [erroSalvar, setErroSalvar] = useState('')

  const carregar = useCallback(async () => {
    try {
      const c = await qa.configuracoes()
      setConfig(c)
      setValores(Object.fromEntries(CAMPOS.map((f) => [f.chave, String(c[f.chave])])))
      if (podeConfigurar) setAuditoria(await qa.auditoria(100))
      setErro('')
    } catch (e) {
      setErro(e.message)
    }
  }, [podeConfigurar])
  useEffect(() => { carregar() }, [carregar])

  const invalidos = CAMPOS.filter((f) => {
    const n = Number(valores[f.chave])
    return !Number.isInteger(n) || n < 1 || n > (config?.limites[f.max] || 1)
  }).map((f) => f.chave)
  const alterado = config && CAMPOS.some((f) => Number(valores[f.chave]) !== config[f.chave])

  async function salvar(e) {
    e.preventDefault()
    setSalvando(true)
    setMensagem('')
    setErroSalvar('')
    try {
      const novo = await qa.alterarConfiguracoes(Object.fromEntries(CAMPOS.map((f) => [f.chave, Number(valores[f.chave])])))
      setConfig(novo)
      setMensagem('Configurações salvas. Valem para as próximas execuções.')
      setAuditoria(await qa.auditoria(100))
    } catch (er) {
      setErroSalvar(er.message)
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div className="pagina qa-pagina">
      <CabecalhoQa titulo="Configurações" descricao="Limites do executor de testes, permissões e registro de quem fez o quê." />
      {erro && <ErroCarga erro={erro} tentarDeNovo={carregar} />}
      {!config && !erro && <div className="painel qa-bloco"><Esqueleto linhas={6} /></div>}

      {config && (
        <div className="qa-layout-config">
          <form className="painel qa-bloco qa-form" onSubmit={salvar}>
            <header className="qa-bloco-topo">
              <h2>Executor</h2>
              <span>{config.atualizadaPor ? `Alterado por ${config.atualizadaPor} em ${dataHoraCurta(config.atualizadaEm)}` : 'Valores padrão do servidor'}</span>
            </header>
            {!podeConfigurar && (
              <div className="mensagem mensagem-neutra"><Icone nome="cadeado" />Somente leitura: alterar exige a permissão qa.configure.</div>
            )}
            <div className="qa-config-campos">
              {CAMPOS.map((f) => (
                <label key={f.chave} className="qa-campo-texto">
                  <span>{f.rotulo}</span>
                  <span className="qa-campo-unidade">
                    <input type="number" min={1} max={config.limites[f.max]} inputMode="numeric" disabled={!podeConfigurar}
                           value={valores[f.chave] ?? ''} aria-invalid={invalidos.includes(f.chave)}
                           onChange={(e) => setValores({ ...valores, [f.chave]: e.target.value })} />
                    <small>{f.unidade} · máx. {config.limites[f.max]}</small>
                  </span>
                  <small className={invalidos.includes(f.chave) ? 'qa-campo-erro' : 'qa-ajuda'}>
                    {invalidos.includes(f.chave) ? `Use um número entre 1 e ${config.limites[f.max]}.` : f.ajuda}
                  </small>
                </label>
              ))}
            </div>
            <p className="qa-ajuda">Execuções simultâneas no servidor: <b className="tabular">{config.limites.execucoesSimultaneas}</b> (definido na configuração do servidor).</p>
            {mensagem && <div className="mensagem mensagem-neutra" role="status"><Icone nome="check" />{mensagem}</div>}
            {erroSalvar && <div className="mensagem mensagem-erro" role="alert"><Icone nome="alerta" />{erroSalvar}</div>}
            {podeConfigurar && (
              <div className="dialogo-acoes">
                <button type="button" className="botao botao-secundario" disabled={!alterado || salvando}
                        onClick={() => setValores(Object.fromEntries(CAMPOS.map((f) => [f.chave, String(config[f.chave])])))}>Desfazer</button>
                <button type="submit" className="botao botao-primario" disabled={!alterado || invalidos.length > 0 || salvando}>
                  {salvando ? 'Salvando…' : 'Salvar alterações'}
                </button>
              </div>
            )}
          </form>

          <aside className="painel qa-bloco qa-config">
            <h2>Suas permissões</h2>
            <ul className="qa-permissoes">
              {PERMISSOES.map(([p, texto]) => (
                <li key={p} className={pode(p) ? 'tem' : ''}>
                  <Icone nome={pode(p) ? 'check' : 'x'} tamanho={15} />
                  <span><code>{p}</code>{texto}</span>
                </li>
              ))}
            </ul>
            <h2>Proteções sempre ativas</h2>
            <ul className="qa-politicas">
              <li>O executor só chama endpoints do catálogo, nos ambientes cadastrados no servidor. Nenhuma URL vem do navegador.</li>
              <li>Não existe ambiente de produção no QA Center.</li>
              <li>Os testes usam uma conta de teste própria e dados gerados (marcados “QA-”), nunca documentos reais.</li>
              <li>Tokens, senhas, CPF, CNPJ, e-mails e telefones são mascarados antes de serem salvos.</li>
              <li>Toda permissão é conferida no servidor (403), não só na tela.</li>
            </ul>
          </aside>
        </div>
      )}

      {podeConfigurar && (
        <section className="painel qa-bloco">
          <header className="qa-bloco-topo"><h2>Auditoria</h2><span>Últimos 100 registros</span></header>
          {!auditoria && <Esqueleto linhas={4} />}
          {auditoria && auditoria.length === 0 && <p className="qa-vazio-curto">Nenhuma ação registrada ainda.</p>}
          {auditoria && auditoria.length > 0 && (
            <div className="qa-tabela-rolagem">
              <table className="qa-tabela">
                <thead><tr><th>Quando</th><th>Quem</th><th>Ação</th><th>Detalhe</th></tr></thead>
                <tbody>
                  {auditoria.map((a) => (
                    <tr key={a.id}>
                      <td className="tabular suave">{dataHoraCurta(a.dataHora)}</td>
                      <td>{a.usuario}</td>
                      <td><span className={`qa-acao qa-acao-${a.acao.toLowerCase()}`}>{ACOES[a.acao] || a.acao}</span></td>
                      <td className="suave">{a.detalhe}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}
    </div>
  )
}
