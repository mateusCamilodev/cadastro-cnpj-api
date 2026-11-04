import { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { qa } from '../../api.js'
import { useAuth } from '../../auth.jsx'
import Icone from '../../components/Icone.jsx'
import {
  CabecalhoQa, Dialogo, ErroCarga, Esqueleto, MetodoBadge, PainelLateral, QaStatus, dataHoraCurta,
} from '../../components/qa/QaComum.jsx'

// Test Suites: coleções reutilizáveis de cenários. As "padrão" vêm do código (somente leitura, mas podem ser
// duplicadas); as demais são criadas/alteradas por quem tem qa.configure.
export default function QaSuites() {
  const { pode } = useAuth()
  const podeConfigurar = pode('qa.configure')
  const navegar = useNavigate()
  const { state } = useLocation()
  const [suites, setSuites] = useState(null)
  const [cenarios, setCenarios] = useState([])
  const [erro, setErro] = useState('')
  const [editor, setEditor] = useState(null)        // { id?, nome, descricao, cenarioIds:Set }
  const [excluindo, setExcluindo] = useState(null)
  const [aviso, setAviso] = useState('')

  const carregar = useCallback(async () => {
    try {
      const [s, c] = await Promise.all([qa.suites(), qa.cenarios()])
      setSuites(s)
      setCenarios(c)
      setErro('')
    } catch (e) {
      setErro(e.message)
    }
  }, [])
  useEffect(() => { carregar() }, [carregar])

  // Vindo de "Cenários → Salvar como Test Suite": abre o editor já com os cenários marcados
  useEffect(() => {
    if (state?.novaCom?.length && podeConfigurar) {
      setEditor({ nome: '', descricao: '', cenarioIds: new Set(state.novaCom) })
    }
  }, [state, podeConfigurar])

  const porId = useMemo(() => new Map(cenarios.map((c) => [c.id, c])), [cenarios])

  async function excluir() {
    try {
      await qa.excluirSuite(excluindo.id)
      setAviso(`Test Suite “${excluindo.nome}” excluída. As execuções antigas continuam no histórico.`)
      setExcluindo(null)
      carregar()
    } catch (e) {
      setAviso('')
      setErro(e.message)
      setExcluindo(null)
    }
  }

  return (
    <div className="pagina qa-pagina">
      <CabecalhoQa titulo="Test Suites" descricao="Coleções reutilizáveis de cenários. As suítes padrão vêm prontas; você pode duplicá-las e montar as suas.">
        {podeConfigurar && (
          <button type="button" className="botao botao-primario" onClick={() => setEditor({ nome: '', descricao: '', cenarioIds: new Set() })}>
            <Icone nome="mais" tamanho={16} />Nova Test Suite
          </button>
        )}
      </CabecalhoQa>

      {erro && <ErroCarga erro={erro} tentarDeNovo={carregar} />}
      {aviso && <div className="mensagem mensagem-neutra" role="status"><Icone nome="check" />{aviso}</div>}
      {!suites && !erro && <div className="painel qa-bloco"><Esqueleto linhas={6} altura={22} /></div>}

      {suites && (
        <div className="qa-suites">
          {suites.map((s) => {
            const lista = s.cenarioIds.map((id) => porId.get(id)).filter(Boolean)
            const metodos = [...new Set(lista.map((c) => c.metodo))]
            return (
              <article key={s.id} className="painel qa-suite">
                <header>
                  <div>
                    <h2>{s.nome}{s.padrao && <span className="qa-tag-neutra">padrão</span>}</h2>
                    {s.descricao && <p>{s.descricao}</p>}
                  </div>
                </header>
                <div className="qa-suite-numeros">
                  <span><b className="tabular">{s.cenarioIds.length}</b> cenário(s)</span>
                  <span className="qa-suite-metodos">{metodos.map((m) => <MetodoBadge key={m} metodo={m} />)}</span>
                </div>
                <div className="qa-suite-ultima">
                  {s.ultimoStatus
                    ? <><QaStatus status={s.ultimoStatus} tipo="execucao" /><span className="suave tabular">{dataHoraCurta(s.ultimaEm)} · {s.execucoes} execução(ões)</span></>
                    : <span className="suave">Nunca executada</span>}
                </div>
                <details className="qa-suite-lista">
                  <summary>Ver cenários</summary>
                  <ul>
                    {lista.map((c) => <li key={c.id}><span className="mono qa-id-inline">{c.id}</span>{c.nome}</li>)}
                  </ul>
                </details>
                <footer>
                  {pode('qa.execute') && (
                    <button type="button" className="botao botao-primario botao-compacto"
                            onClick={() => navegar('/qa/nova', { state: { modo: 'suite', suiteId: s.id } })}>
                      <Icone nome="play" tamanho={14} />Executar
                    </button>
                  )}
                  <button type="button" className="botao botao-fantasma botao-compacto"
                          onClick={() => navegar('/qa/execucoes', { state: { suite: s.id } })}>Histórico</button>
                  {podeConfigurar && (
                    <span className="qa-suite-acoes">
                      <button type="button" className="botao-icone-qa" title="Duplicar" aria-label={`Duplicar ${s.nome}`}
                              onClick={() => setEditor({ nome: `${s.nome} (cópia)`, descricao: s.descricao || '', cenarioIds: new Set(s.cenarioIds) })}>
                        <Icone nome="copiar" tamanho={16} />
                      </button>
                      {!s.padrao && (
                        <>
                          <button type="button" className="botao-icone-qa" title="Editar" aria-label={`Editar ${s.nome}`}
                                  onClick={() => setEditor({ id: s.id, nome: s.nome, descricao: s.descricao || '', cenarioIds: new Set(s.cenarioIds) })}>
                            <Icone nome="lapis" tamanho={16} />
                          </button>
                          <button type="button" className="botao-icone-qa perigo" title="Excluir" aria-label={`Excluir ${s.nome}`}
                                  onClick={() => setExcluindo(s)}>
                            <Icone nome="lixeira" tamanho={16} />
                          </button>
                        </>
                      )}
                    </span>
                  )}
                </footer>
              </article>
            )
          })}
        </div>
      )}

      <EditorSuite editor={editor} cenarios={cenarios} aoFechar={() => setEditor(null)}
                   aoSalvar={(nome) => { setEditor(null); setAviso(`Test Suite “${nome}” salva.`); carregar() }} />

      <Dialogo aberto={!!excluindo} titulo="Excluir Test Suite?" aoFechar={() => setExcluindo(null)}
               acoes={<>
                 <button type="button" className="botao botao-secundario" onClick={() => setExcluindo(null)}>Manter</button>
                 <button type="button" className="botao botao-perigo" onClick={excluir}>Excluir</button>
               </>}>
        <p><strong>{excluindo?.nome}</strong> deixa de aparecer para novas execuções. As execuções já feitas com ela continuam no histórico.</p>
      </Dialogo>
    </div>
  )
}

function EditorSuite({ editor, cenarios, aoFechar, aoSalvar }) {
  const [nome, setNome] = useState('')
  const [descricao, setDescricao] = useState('')
  const [marcados, setMarcados] = useState(() => new Set())
  const [busca, setBusca] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')

  useEffect(() => {
    if (!editor) return
    setNome(editor.nome)
    setDescricao(editor.descricao)
    setMarcados(new Set(editor.cenarioIds))
    setBusca('')
    setErro('')
  }, [editor])

  const visiveis = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    return cenarios.filter((c) => !termo || `${c.id} ${c.nome} ${c.caminho} ${c.categoriaRotulo}`.toLowerCase().includes(termo))
  }, [cenarios, busca])

  function alternar(id) {
    const novo = new Set(marcados)
    if (novo.has(id)) novo.delete(id); else novo.add(id)
    setMarcados(novo)
  }

  async function salvar(e) {
    e.preventDefault()
    setSalvando(true)
    setErro('')
    // Mantém a ordem do catálogo
    const corpo = { nome, descricao, cenarioIds: cenarios.filter((c) => marcados.has(c.id)).map((c) => c.id) }
    try {
      if (editor.id) await qa.alterarSuite(editor.id, corpo)
      else await qa.criarSuite(corpo)
      aoSalvar(nome.trim())
    } catch (er) {
      setErro(er.message)
    } finally {
      setSalvando(false)
    }
  }

  return (
    <PainelLateral aberto={!!editor} aoFechar={aoFechar} largura={600}
                   titulo={editor?.id ? 'Editar Test Suite' : 'Nova Test Suite'} subtitulo={`${marcados.size} cenário(s) selecionado(s)`}>
      {editor && (
        <form className="qa-form" onSubmit={salvar}>
          <label className="qa-campo-texto">
            <span>Nome</span>
            <input value={nome} onChange={(e) => setNome(e.target.value)} maxLength={120} required minLength={3} placeholder="Ex.: Regressão antes do deploy" />
          </label>
          <label className="qa-campo-texto">
            <span>Descrição (opcional)</span>
            <textarea value={descricao} onChange={(e) => setDescricao(e.target.value)} maxLength={500} rows={2} />
          </label>
          <div className="qa-campo-texto">
            <span>Cenários</span>
            <label className="busca">
              <Icone nome="busca" tamanho={16} />
              <input value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Buscar cenário" aria-label="Buscar cenário" />
            </label>
            <ul className="qa-cenarios-escolha qa-cenarios-rolagem">
              {visiveis.map((c) => (
                <li key={c.id}>
                  <label>
                    <input type="checkbox" checked={marcados.has(c.id)} onChange={() => alternar(c.id)} />
                    <span className="mono qa-id-inline">{c.id}</span>
                    <span>{c.nome}</span>
                    <MetodoBadge metodo={c.metodo} />
                  </label>
                </li>
              ))}
            </ul>
          </div>
          {erro && <div className="mensagem mensagem-erro" role="alert"><Icone nome="alerta" />{erro}</div>}
          <div className="dialogo-acoes">
            <button type="button" className="botao botao-secundario" onClick={aoFechar} disabled={salvando}>Cancelar</button>
            <button type="submit" className="botao botao-primario" disabled={salvando || !marcados.size || nome.trim().length < 3}>
              {salvando ? 'Salvando…' : 'Salvar Test Suite'}
            </button>
          </div>
        </form>
      )}
    </PainelLateral>
  )
}
