import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { qa } from '../../api.js'
import { useAuth } from '../../auth.jsx'
import Icone from '../../components/Icone.jsx'
import MascoteDuvida from '../../components/MascoteDuvida.jsx'
import { CabecalhoQa, ErroCarga, Esqueleto, MetodoBadge, QaStatus, dataHoraCurta } from '../../components/qa/QaComum.jsx'

const METODOS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE']

// Catálogo de endpoints (controlado no backend: o projeto não tem Swagger/OpenAPI).
// Seleção por checkbox individual ou por grupo; a seleção segue para "Nova execução".
export default function QaEndpoints() {
  const { pode } = useAuth()
  const navegar = useNavigate()
  const [catalogo, setCatalogo] = useState(null)
  const [erro, setErro] = useState('')
  const [selecionados, setSelecionados] = useState(() => new Set())
  const [busca, setBusca] = useState('')
  const [metodos, setMetodos] = useState(() => new Set())
  const [categoriaFiltro, setCategoriaFiltro] = useState('')

  const carregar = useCallback(async () => {
    try {
      const [endpoints, cats, ambientes] = await Promise.all([qa.endpoints(), qa.categorias(), qa.ambientes()])
      setCatalogo({ endpoints, categorias: cats, ambientes })
      setErro('')
    } catch (e) {
      setErro(e.message)
    }
  }, [])
  useEffect(() => { carregar() }, [carregar])

  const visiveis = useMemo(() => {
    if (!catalogo) return []
    const termo = busca.trim().toLowerCase()
    return catalogo.endpoints.filter((e) =>
      (!termo || `${e.nome} ${e.caminho} ${e.descricao}`.toLowerCase().includes(termo))
      && (!metodos.size || metodos.has(e.metodo))
      && (!categoriaFiltro || e.categorias.includes(categoriaFiltro)))
  }, [catalogo, busca, metodos, categoriaFiltro])

  const grupos = useMemo(() => {
    const mapa = new Map()
    visiveis.forEach((e) => mapa.set(e.grupo, [...(mapa.get(e.grupo) || []), e]))
    return [...mapa.entries()]
  }, [visiveis])

  const rotuloCategoria = (id) => catalogo?.categorias.find((c) => c.id === id)?.rotulo.replace(/^Testes de /, '') || id
  const temFiltro = busca || metodos.size || categoriaFiltro

  function alternar(conjunto, setConjunto, valor) {
    const novo = new Set(conjunto)
    if (novo.has(valor)) novo.delete(valor); else novo.add(valor)
    setConjunto(novo)
  }

  function alternarGrupo(lista) {
    const novo = new Set(selecionados)
    const todos = lista.every((e) => novo.has(e.id))
    lista.forEach((e) => (todos ? novo.delete(e.id) : novo.add(e.id)))
    setSelecionados(novo)
  }

  return (
    <div className="pagina qa-pagina">
      <CabecalhoQa titulo="Endpoints" descricao="Catálogo controlado no backend com os endpoints reais do Carimbô. O executor só consegue chamar o que está aqui.">
        {pode('qa.execute') && (
          <button type="button" className="botao botao-primario" disabled={!selecionados.size}
                  onClick={() => navegar('/qa/nova', { state: { modo: 'endpoints', endpointIds: [...selecionados] } })}>
            <Icone nome="play" tamanho={16} />Configurar execução{selecionados.size ? ` (${selecionados.size})` : ''}
          </button>
        )}
      </CabecalhoQa>

      {erro && <ErroCarga erro={erro} tentarDeNovo={carregar} />}

      <section className="painel qa-bloco">
        <div className="qa-filtros">
          <label className="busca">
            <Icone nome="busca" tamanho={16} />
            <input value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Buscar por nome ou caminho" aria-label="Buscar endpoint" />
          </label>
          <div className="qa-chips" role="group" aria-label="Filtrar por método">
            {METODOS.map((m) => (
              <button key={m} type="button" className={`qa-chip${metodos.has(m) ? ' ativo' : ''}`} aria-pressed={metodos.has(m)}
                      onClick={() => alternar(metodos, setMetodos, m)}>{m}</button>
            ))}
          </div>
          <select value={categoriaFiltro} onChange={(e) => setCategoriaFiltro(e.target.value)} aria-label="Filtrar por categoria" className="qa-select">
            <option value="">Todas as categorias</option>
            {catalogo?.categorias.filter((c) => c.cenarios > 0).map((c) => <option key={c.id} value={c.id}>{c.rotulo}</option>)}
          </select>
          {temFiltro ? (
            <button type="button" className="botao botao-fantasma botao-compacto" onClick={() => { setBusca(''); setMetodos(new Set()); setCategoriaFiltro('') }}>
              Limpar filtros
            </button>
          ) : null}
          {selecionados.size > 0 && (
            <button type="button" className="botao botao-fantasma botao-compacto" onClick={() => setSelecionados(new Set())}>
              Limpar seleção ({selecionados.size})
            </button>
          )}
        </div>

        {!catalogo && !erro && <Esqueleto linhas={7} altura={22} />}
        {catalogo && grupos.length === 0 && (
          <div className="vazio"><MascoteDuvida /><strong>Nenhum endpoint com esses filtros</strong><span>Ajuste a busca ou os filtros.</span></div>
        )}

        {grupos.map(([grupo, lista]) => {
          const marcados = lista.filter((e) => selecionados.has(e.id)).length
          return (
            <div key={grupo} className="qa-grupo">
              <label className="qa-grupo-topo">
                <input type="checkbox" checked={marcados === lista.length}
                       ref={(el) => { if (el) el.indeterminate = marcados > 0 && marcados < lista.length }}
                       onChange={() => alternarGrupo(lista)} aria-label={`Selecionar o grupo ${grupo}`} />
                <strong>{grupo}</strong>
                <span className="suave">{lista.length} endpoint(s)</span>
              </label>
              <ul className="qa-endpoints">
                {lista.map((e) => (
                  <li key={e.id} className={selecionados.has(e.id) ? 'selecionado' : ''}>
                    <label>
                      <input type="checkbox" checked={selecionados.has(e.id)} onChange={() => alternar(selecionados, setSelecionados, e.id)} />
                      <span className="qa-endpoint-principal">
                        <span className="qa-endpoint-linha">
                          <MetodoBadge metodo={e.metodo} />
                          <code className="qa-caminho">{e.caminho}</code>
                          {e.modificaDados && <span className="qa-tag">cria/altera dados de teste</span>}
                        </span>
                        <span className="qa-endpoint-nome">{e.nome}</span>
                        <span className="qa-endpoint-desc">{e.descricao}</span>
                        <span className="qa-endpoint-tags">
                          {e.categorias.map((c) => <span key={c} className="qa-tag-neutra">{rotuloCategoria(c)}</span>)}
                        </span>
                      </span>
                      <span className="qa-endpoint-meta">
                        <span className="suave">
                          Ambientes: {e.ambientes.map((a) => {
                            const amb = catalogo.ambientes.find((x) => x.id === a)
                            return amb ? `${amb.rotulo}${amb.disponivel ? '' : ' (fora do ar)'}` : a
                          }).join(', ')}
                        </span>
                        <span className="suave tabular">{e.cenarios} cenário(s) · {e.execucoes} resultado(s)</span>
                        {e.ultimoResultado
                          ? <span className="qa-endpoint-ultimo"><QaStatus status={e.ultimoResultado} /><span className="suave">{dataHoraCurta(e.ultimoEm)}</span></span>
                          : <span className="suave">Nunca testado</span>}
                      </span>
                    </label>
                  </li>
                ))}
              </ul>
            </div>
          )
        })}
      </section>
      <p className="qa-ajuda">Quer escolher cenários um a um ou usar uma coleção pronta? Veja <Link className="qa-link" to="/qa/cenarios">Cenários</Link> e <Link className="qa-link" to="/qa/suites">Test Suites</Link>.</p>
    </div>
  )
}
