import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { qa } from '../../api.js'
import { useAuth } from '../../auth.jsx'
import Icone from '../../components/Icone.jsx'
import MascoteDuvida from '../../components/MascoteDuvida.jsx'
import {
  CRITICIDADE, CabecalhoQa, ErroCarga, Esqueleto, MetodoBadge, PainelLateral, QaStatus, dataHoraCurta,
} from '../../components/qa/QaComum.jsx'

// Catálogo de cenários automatizados: o que cada teste faz, pré-condições, resultado esperado e último resultado.
// Os cenários marcados podem ser executados na hora ou salvos como uma Test Suite.
export default function QaCenarios() {
  const { pode } = useAuth()
  const navegar = useNavigate()
  const [cenarios, setCenarios] = useState(null)
  const [categorias, setCategorias] = useState([])
  const [erro, setErro] = useState('')
  const [busca, setBusca] = useState('')
  const [categoria, setCategoria] = useState('')
  const [ultimo, setUltimo] = useState('')
  const [marcados, setMarcados] = useState(() => new Set())
  const [aberto, setAberto] = useState(null)

  const carregar = useCallback(async () => {
    try {
      const [c, cats] = await Promise.all([qa.cenarios(), qa.categorias()])
      setCenarios(c)
      setCategorias(cats)
      setErro('')
    } catch (e) {
      setErro(e.message)
    }
  }, [])
  useEffect(() => { carregar() }, [carregar])

  const visiveis = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    return (cenarios || []).filter((c) =>
      (!categoria || c.categoria === categoria)
      && (!ultimo || (ultimo === 'NUNCA' ? !c.ultimoResultado : c.ultimoResultado === ultimo))
      && (!termo || `${c.id} ${c.nome} ${c.descricao} ${c.caminho}`.toLowerCase().includes(termo)))
  }, [cenarios, busca, categoria, ultimo])

  const todosVisiveisMarcados = visiveis.length > 0 && visiveis.every((c) => marcados.has(c.id))

  function alternar(id) {
    const novo = new Set(marcados)
    if (novo.has(id)) novo.delete(id); else novo.add(id)
    setMarcados(novo)
  }

  function alternarVisiveis() {
    const novo = new Set(marcados)
    visiveis.forEach((c) => (todosVisiveisMarcados ? novo.delete(c.id) : novo.add(c.id)))
    setMarcados(novo)
  }

  return (
    <div className="pagina qa-pagina">
      <CabecalhoQa titulo="Cenários de teste" descricao="Cada cenário chama a API de verdade e confere status HTTP, mensagens e contrato da resposta.">
        {pode('qa.configure') && (
          <button type="button" className="botao botao-secundario" disabled={!marcados.size}
                  onClick={() => navegar('/qa/suites', { state: { novaCom: [...marcados] } })}>
            <Icone nome="pastas" tamanho={16} />Salvar como Test Suite
          </button>
        )}
        {pode('qa.execute') && (
          <button type="button" className="botao botao-primario" disabled={!marcados.size}
                  onClick={() => navegar('/qa/nova', { state: { modo: 'cenarios', cenarioIds: [...marcados] } })}>
            <Icone nome="play" tamanho={16} />Executar{marcados.size ? ` ${marcados.size}` : ''} selecionado(s)
          </button>
        )}
      </CabecalhoQa>
      {erro && <ErroCarga erro={erro} tentarDeNovo={carregar} />}

      <section className="painel qa-bloco">
        <div className="qa-filtros">
          <label className="busca">
            <Icone nome="busca" tamanho={16} />
            <input value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Buscar cenário" aria-label="Buscar cenário" />
          </label>
          <select className="qa-select" value={categoria} onChange={(e) => setCategoria(e.target.value)} aria-label="Categoria">
            <option value="">Todas as categorias</option>
            {categorias.filter((c) => c.cenarios > 0).map((c) => <option key={c.id} value={c.id}>{c.rotulo} ({c.cenarios})</option>)}
          </select>
          <select className="qa-select" value={ultimo} onChange={(e) => setUltimo(e.target.value)} aria-label="Último resultado">
            <option value="">Qualquer último resultado</option>
            <option value="PASSED">Aprovado</option>
            <option value="FAILED">Reprovado</option>
            <option value="ERROR">Erro</option>
            <option value="SKIPPED">Bloqueado</option>
            <option value="NUNCA">Nunca executado</option>
          </select>
          {cenarios && <span className="suave tabular">{visiveis.length} de {cenarios.length}</span>}
          {marcados.size > 0 && (
            <button type="button" className="botao botao-fantasma botao-compacto" onClick={() => setMarcados(new Set())}>Limpar seleção ({marcados.size})</button>
          )}
        </div>

        {!cenarios && !erro && <Esqueleto linhas={8} altura={22} />}
        {cenarios && !visiveis.length && (
          <div className="vazio"><MascoteDuvida /><strong>Nenhum cenário encontrado</strong><span>Ajuste a busca ou os filtros.</span></div>
        )}
        {visiveis.length > 0 && (
          <div className="qa-tabela-rolagem">
            <table className="qa-tabela">
              <thead>
                <tr>
                  <th className="qa-col-check">
                    <input type="checkbox" checked={todosVisiveisMarcados} onChange={alternarVisiveis} aria-label="Selecionar todos os cenários visíveis" />
                  </th>
                  <th>Cenário</th><th>Endpoint</th><th>Categoria</th><th>Criticidade</th><th>Último resultado</th>
                </tr>
              </thead>
              <tbody>
                {visiveis.map((c) => (
                  <tr key={c.id} className={`qa-linha${marcados.has(c.id) ? ' qa-linha-marcada' : ''}`}>
                    <td className="qa-col-check">
                      <input type="checkbox" checked={marcados.has(c.id)} onChange={() => alternar(c.id)} aria-label={`Selecionar ${c.id}`} />
                    </td>
                    <td>
                      <button type="button" className="qa-celula-botao" onClick={() => setAberto(c)}>
                        <span className="mono qa-id">{c.id}</span><span className="qa-cenario-nome">{c.nome}</span>
                      </button>
                    </td>
                    <td><MetodoBadge metodo={c.metodo} /> <code className="qa-caminho">{c.caminho}</code></td>
                    <td>{c.categoriaRotulo}</td>
                    <td><span className={`qa-criticidade qa-criticidade-${(c.criticidade || '').toLowerCase()}`}>{CRITICIDADE[c.criticidade] || c.criticidade}</span></td>
                    <td>
                      {c.ultimoResultado
                        ? <span className="qa-endpoint-ultimo"><QaStatus status={c.ultimoResultado} /><span className="suave tabular">{dataHoraCurta(c.ultimoEm)}</span></span>
                        : <span className="suave">Nunca executado</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <PainelLateral aberto={!!aberto} aoFechar={() => setAberto(null)} titulo={aberto ? `${aberto.id} · ${aberto.nome}` : ''}
                     subtitulo={aberto ? `${aberto.metodo} ${aberto.caminho}` : ''}>
        {aberto && (
          <dl className="qa-detalhe-info">
            <dt>O que testa</dt><dd>{aberto.descricao}</dd>
            <dt>Endpoint</dt><dd>{aberto.endpointNome}</dd>
            <dt>Categoria</dt><dd>{aberto.categoriaRotulo}</dd>
            <dt>Criticidade</dt><dd>{CRITICIDADE[aberto.criticidade] || aberto.criticidade}</dd>
            <dt>Pré-condições</dt><dd>{aberto.preCondicoes || 'Nenhuma'}</dd>
            <dt>Resultado esperado</dt><dd>{aberto.esperado}</dd>
            <dt>Tempo limite</dt><dd>{aberto.timeoutSegundos} s</dd>
            <dt>Último resultado</dt><dd>{aberto.ultimoResultado ? <QaStatus status={aberto.ultimoResultado} /> : 'Nunca executado'}</dd>
          </dl>
        )}
      </PainelLateral>
    </div>
  )
}
