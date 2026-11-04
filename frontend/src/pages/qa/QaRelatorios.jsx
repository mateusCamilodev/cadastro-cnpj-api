import { useCallback, useEffect, useState } from 'react'
import { qa } from '../../api.js'
import Icone from '../../components/Icone.jsx'
import { CabecalhoQa, ErroCarga, Esqueleto, Kpi, duracao } from '../../components/qa/QaComum.jsx'
import { BarraDistribuicao, BarrasFalhas, GraficoTendencia } from '../../components/qa/QaGraficos.jsx'

// Relatórios: indicadores do período, tendência, falhas por endpoint/categoria, exportação CSV e PDF (impressão)
export default function QaRelatorios() {
  const [periodo, setPeriodo] = useState({ de: '', ate: '' })
  const [agrupamento, setAgrupamento] = useState('dia')
  const [dados, setDados] = useState(null)
  const [erro, setErro] = useState('')
  const [exportando, setExportando] = useState(false)

  const carregar = useCallback(async () => {
    try {
      const [resumo, tendencias, falhas] = await Promise.all([
        qa.resumo(periodo),
        qa.tendencias({ agrupamento, quantidade: agrupamento === 'dia' ? 14 : 24 }),
        qa.falhas(periodo),
      ])
      setDados({ resumo, tendencias, falhas })
      setErro('')
    } catch (e) {
      setErro(e.message)
    }
  }, [periodo, agrupamento])
  useEffect(() => { carregar() }, [carregar])

  async function exportarCsv() {
    setExportando(true)
    try {
      await qa.baixarCsv(periodo)
    } catch (e) {
      setErro(e.message)
    } finally {
      setExportando(false)
    }
  }

  const r = dados?.resumo
  const textoPeriodo = periodo.de || periodo.ate
    ? `${periodo.de ? new Date(periodo.de + 'T00:00').toLocaleDateString('pt-BR') : 'início'} a ${periodo.ate ? new Date(periodo.ate + 'T00:00').toLocaleDateString('pt-BR') : 'hoje'}`
    : 'Todo o histórico'

  return (
    <div className="pagina qa-pagina qa-relatorio">
      <CabecalhoQa titulo="Relatórios" descricao={`Indicadores calculados a partir das execuções reais · ${textoPeriodo}`}>
        <button type="button" className="botao botao-secundario" onClick={exportarCsv} disabled={exportando || !r?.testes}>
          <Icone nome="download" tamanho={16} />{exportando ? 'Exportando…' : 'Exportar CSV'}
        </button>
        <button type="button" className="botao botao-secundario" onClick={() => window.print()} disabled={!r?.testes}>
          <Icone nome="impressora" tamanho={16} />Salvar PDF
        </button>
      </CabecalhoQa>

      <div className="qa-filtros qa-filtros-solto">
        <label className="qa-data">De <input type="date" value={periodo.de} onChange={(e) => setPeriodo({ ...periodo, de: e.target.value })} /></label>
        <label className="qa-data">Até <input type="date" value={periodo.ate} onChange={(e) => setPeriodo({ ...periodo, ate: e.target.value })} /></label>
        {(periodo.de || periodo.ate) && (
          <button type="button" className="botao botao-fantasma botao-compacto" onClick={() => setPeriodo({ de: '', ate: '' })}>Todo o histórico</button>
        )}
      </div>

      {erro && <ErroCarga erro={erro} tentarDeNovo={carregar} />}
      {!dados && !erro && <div className="painel qa-bloco"><Esqueleto linhas={8} /></div>}

      {r && (
        <>
          <section className="kpis kpis-8" aria-label="Indicadores do período">
            <Kpi rotulo="Execuções" valor={r.execucoes} detalhe={r.emAndamento ? `${r.emAndamento} em andamento` : null} />
            <Kpi rotulo="Testes" valor={r.testes} />
            <Kpi rotulo="Taxa de sucesso" valor={r.taxaSucesso === null ? '—' : `${r.taxaSucesso.toLocaleString('pt-BR')}%`}
                 detalhe="aprovados ÷ (aprovados + reprovados + erros)"
                 destaque={r.taxaSucesso === null ? null : r.taxaSucesso >= 95 ? 'bom' : r.taxaSucesso >= 80 ? 'atencao' : 'ruim'} />
            <Kpi rotulo="Reprovados" valor={r.reprovados} destaque={r.reprovados ? 'ruim' : null} />
            <Kpi rotulo="Erros" valor={r.erros} detalhe="falha técnica ou tempo esgotado" />
            <Kpi rotulo="Bloqueados / ignorados" valor={r.pulados + r.cancelados} detalhe={`${r.pulados} bloqueados · ${r.cancelados} cancelados`} />
            <Kpi rotulo="Cobertura de endpoints" valor={r.coberturaEndpoints === null ? '—' : `${r.coberturaEndpoints.toLocaleString('pt-BR')}%`}
                 detalhe={`${r.endpointsTestados} de ${r.endpointsTotal} · ${r.cenariosTestados}/${r.cenariosTotal} cenários`} />
            <Kpi rotulo="Tempo médio" valor={duracao(r.tempoMedioMs)} detalhe="por execução" />
          </section>

          <section className="painel qa-bloco">
            <header className="qa-bloco-topo">
              <h2>Tendência</h2>
              <div className="qa-chips" role="group" aria-label="Agrupamento">
                {[['dia', 'Por dia (14)'], ['hora', 'Por hora (24)']].map(([v, l]) => (
                  <button key={v} type="button" className={`qa-chip${agrupamento === v ? ' ativo' : ''}`} aria-pressed={agrupamento === v}
                          onClick={() => setAgrupamento(v)}>{l}</button>
                ))}
              </div>
            </header>
            <GraficoTendencia pontos={dados.tendencias} />
          </section>

          <div className="qa-grade-2">
            <section className="painel qa-bloco">
              <header className="qa-bloco-topo"><h2>Falhas por endpoint</h2><span>reprovados + erros</span></header>
              <BarrasFalhas grupos={dados.falhas.porEndpoint} vazio="Nenhum endpoint falhou no período." />
            </section>
            <section className="painel qa-bloco">
              <header className="qa-bloco-topo"><h2>Falhas por categoria</h2><span>reprovados + erros</span></header>
              <BarrasFalhas grupos={dados.falhas.porCategoria} vazio="Nenhuma categoria falhou no período." />
            </section>
          </div>

          <section className="painel qa-bloco">
            <header className="qa-bloco-topo"><h2>Distribuição dos resultados</h2></header>
            <BarraDistribuicao valores={r} />
          </section>
        </>
      )}
    </div>
  )
}
