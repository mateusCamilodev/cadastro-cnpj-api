import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { qa } from '../../api.js'
import { useAuth } from '../../auth.jsx'
import Icone from '../../components/Icone.jsx'
import { SimboloMarca } from '../../components/Marca.jsx'
import { CabecalhoQa, ErroCarga, Esqueleto, Kpi, QaStatus, dataHoraCurta, duracao } from '../../components/qa/QaComum.jsx'
import { BarraDistribuicao, BarrasFalhas, GraficoTendencia } from '../../components/qa/QaGraficos.jsx'

// Visão geral do QA Center: indicadores, tendência, distribuição, falhas por endpoint e execuções recentes.
// Tudo vem do backend; atualiza sozinho enquanto houver execução em andamento.
export default function QaPainel() {
  const { pode } = useAuth()
  const comRelatorios = pode('qa.reports.view')
  const [dados, setDados] = useState(null)
  const [erro, setErro] = useState('')

  const carregar = useCallback(async () => {
    try {
      const [recentes, resumo, tendencias, falhas] = await Promise.all([
        qa.execucoes({ tamanho: 6 }),
        comRelatorios ? qa.resumo() : null,
        comRelatorios ? qa.tendencias({ agrupamento: 'dia', quantidade: 14 }) : null,
        comRelatorios ? qa.falhas() : null,
      ])
      setDados({ recentes, resumo, tendencias, falhas })
      setErro('')
    } catch (e) {
      setErro(e.message)
    }
  }, [comRelatorios])

  useEffect(() => { carregar() }, [carregar])

  // Enquanto alguma execução estiver rodando, atualiza a cada 5 s
  const emAndamento = dados?.recentes?.itens?.some((e) => e.status === 'PENDING' || e.status === 'RUNNING')
  useEffect(() => {
    if (!emAndamento) return
    const t = setInterval(carregar, 5000)
    return () => clearInterval(t)
  }, [emAndamento, carregar])

  const r = dados?.resumo
  const semExecucoes = dados && dados.recentes.total === 0

  return (
    <div className="pagina qa-pagina">
      <CabecalhoQa titulo="Dashboard" descricao="Saúde da API do Carimbô, calculada a partir das execuções de teste reais.">
        {pode('qa.execute') && (
          <Link to="/qa/nova" className="botao botao-primario"><Icone nome="play" tamanho={16} />Nova execução</Link>
        )}
      </CabecalhoQa>

      {erro && <ErroCarga erro={erro} tentarDeNovo={carregar} />}
      {!dados && !erro && <div className="painel qa-bloco"><Esqueleto linhas={6} /></div>}

      {semExecucoes && (
        <div className="painel qa-vazio">
          <SimboloMarca tamanho={56} />
          <strong>Nenhuma execução ainda</strong>
          <span>Escolha os endpoints e as categorias de teste para rodar a primeira bateria contra a API.</span>
          {pode('qa.execute') && <Link to="/qa/nova" className="botao botao-primario">Configurar primeira execução</Link>}
        </div>
      )}

      {dados && !semExecucoes && (
        <>
          {r && (
            <section className="kpis kpis-8" aria-label="Indicadores">
              <Kpi rotulo="Testes executados" valor={r.testes} detalhe={`em ${r.execucoes} execução(ões)${r.emAndamento ? ` · ${r.emAndamento} em andamento` : ''}`} />
              <Kpi rotulo="Taxa de sucesso" valor={r.taxaSucesso === null ? '—' : `${r.taxaSucesso.toLocaleString('pt-BR')}%`}
                   detalhe={`aprovados ÷ (aprovados + reprovados + erros)`}
                   destaque={r.taxaSucesso === null ? null : r.taxaSucesso >= 95 ? 'bom' : r.taxaSucesso >= 80 ? 'atencao' : 'ruim'} />
              <Kpi rotulo="Aprovados" valor={r.aprovados} />
              <Kpi rotulo="Reprovados" valor={r.reprovados} detalhe={r.erros ? `+ ${r.erros} com erro técnico` : 'nenhum erro técnico'} destaque={r.reprovados || r.erros ? 'ruim' : null} />
              <Kpi rotulo="Bloqueados" valor={r.pulados} detalhe="pré-condição não atendida" />
              <Kpi rotulo="Ignorados" valor={r.cancelados} detalhe="não rodaram: execução cancelada" />
              <Kpi rotulo="Cobertura de endpoints" valor={r.coberturaEndpoints === null ? '—' : `${r.coberturaEndpoints.toLocaleString('pt-BR')}%`}
                   detalhe={`${r.endpointsTestados} de ${r.endpointsTotal} endpoints · ${r.cenariosTestados}/${r.cenariosTotal} cenários`}
                   destaque={r.coberturaEndpoints === 100 ? 'bom' : r.coberturaEndpoints >= 60 ? 'atencao' : 'ruim'} />
              <Kpi rotulo="Tempo médio" valor={duracao(r.tempoMedioMs)} detalhe="por execução concluída" />
            </section>
          )}

          {r && (
            <div className="qa-grade-2">
              <section className="painel qa-bloco">
                <header className="qa-bloco-topo"><h2>Resultados por dia</h2><span>Últimos 14 dias</span></header>
                <GraficoTendencia pontos={dados.tendencias} />
              </section>
              <section className="painel qa-bloco">
                <header className="qa-bloco-topo"><h2>Distribuição</h2><span>Todos os testes</span></header>
                <BarraDistribuicao valores={r} />
                <header className="qa-bloco-topo qa-bloco-topo-meio"><h2>Endpoints com mais falhas</h2></header>
                <BarrasFalhas grupos={dados.falhas.porEndpoint.slice(0, 5)} vazio="Nenhum endpoint falhou até agora." />
              </section>
            </div>
          )}

          <section className="painel qa-bloco">
            <header className="qa-bloco-topo">
              <h2>Execuções recentes</h2>
              <Link to="/qa/execucoes" className="qa-link">Ver histórico</Link>
            </header>
            <ul className="qa-recentes">
              {dados.recentes.itens.map((e) => (
                <li key={e.id}>
                  <Link to={`/qa/execucoes/${e.id}`}>
                    <span className="mono">#{e.id.slice(0, 8)}</span>
                    <QaStatus status={e.status} tipo="execucao" />
                    <span className="qa-recentes-placar tabular">{e.aprovados}/{e.total} aprovados</span>
                    <span className="suave">{e.suiteNome || e.ambienteRotulo}</span>
                    <span className="suave">{e.usuario}</span>
                    <span className="suave tabular">{dataHoraCurta(e.criadaEm)}</span>
                    <Icone nome="seta" tamanho={16} />
                  </Link>
                </li>
              ))}
            </ul>
          </section>
        </>
      )}
    </div>
  )
}
