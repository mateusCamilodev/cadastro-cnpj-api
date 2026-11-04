import { useRef, useState } from 'react'
import { api } from '../api.js'
import { dataHora, horaCurta } from '../format.js'
import Icone from './Icone.jsx'
import Carregando from './Carregando.jsx'

// Documentos declarados na criação: o que falta, o que já chegou e o envio.
// O envio usa o endpoint existente (POST /solicitacoes/{id}/documento). Todas as regras
// (status, prazo, nome declarado, PDF válido, repetido) continuam no backend; aqui só
// mostramos a resposta dele.
//
// Estados por arquivo: pendente → enviando → recebido | erro (com a mensagem da API)

function mensagemDeErro(e) {
  const detalhe = e.detalhes?.[0]?.mensagem
  return detalhe ? `${e.message}: ${detalhe}` : e.message
}

export default function Documentos({ id, arquivos, status, prazo, aoEnviar }) {
  const [envios, setEnvios] = useState({})        // nomeArquivo -> { estado, mensagem }
  const [avisos, setAvisos] = useState([])        // arquivos soltos que não bateram com nenhum declarado
  const [arrastando, setArrastando] = useState(false)
  const seletores = useRef({})

  const podeEnviar = status === 'AGUARDANDO_DOCUMENTO'
  const pendentes = (arquivos || []).filter((a) => !a.recebido)
  const recebidos = (arquivos || []).length - pendentes.length

  async function enviar(arquivo, nomeDeclarado) {
    const chave = nomeDeclarado || arquivo.name
    setEnvios((v) => ({ ...v, [chave]: { estado: 'enviando' } }))
    try {
      await api.enviarDocumento(id, arquivo)
      setEnvios((v) => ({ ...v, [chave]: { estado: 'ok', mensagem: 'Recebido agora' } }))
      await aoEnviar()
      return true
    } catch (e) {
      if (nomeDeclarado) {
        setEnvios((v) => ({ ...v, [chave]: { estado: 'erro', mensagem: mensagemDeErro(e) } }))
      } else {
        setAvisos((a) => [...a, { nome: arquivo.name, mensagem: mensagemDeErro(e) }])
        setEnvios((v) => { const n = { ...v }; delete n[chave]; return n })
      }
      return false
    }
  }

  // Botão "Enviar" de um item: o nome precisa ser o declarado (avisamos antes de enviar)
  function escolherArquivo(nomeDeclarado, lista) {
    const arquivo = lista?.[0]
    if (!arquivo) return
    if (arquivo.name !== nomeDeclarado) {
      setEnvios((v) => ({ ...v, [nomeDeclarado]: { estado: 'erro', mensagem: `O arquivo precisa se chamar “${nomeDeclarado}” (você escolheu “${arquivo.name}”).` } }))
      return
    }
    enviar(arquivo, nomeDeclarado)
  }

  // Arrastar e soltar: manda cada arquivo; a API decide a qual documento declarado ele pertence
  async function soltar(evento) {
    evento.preventDefault()
    setArrastando(false)
    if (!podeEnviar) return
    setAvisos([])
    for (const arquivo of Array.from(evento.dataTransfer.files)) {
      const declarado = pendentes.find((a) => a.nomeArquivo === arquivo.name)
      await enviar(arquivo, declarado?.nomeArquivo)
    }
  }

  return (
    <section className="painel documentos-painel" aria-labelledby="titulo-documentos">
      <header className="painel-cabecalho">
        <div>
          <h2 id="titulo-documentos">Documentos</h2>
          <p className="painel-sub">
            {arquivos ? `${recebidos} de ${arquivos.length} recebidos` : 'Carregando…'}
            {podeEnviar && prazo && pendentes.length > 0 && <> · envie até {horaCurta(prazo)}</>}
          </p>
        </div>
      </header>

      <ul className="documentos">
        {(arquivos || []).map((a) => {
          const envio = envios[a.nomeArquivo]
          const enviando = envio?.estado === 'enviando'
          const situacao = a.recebido ? 'recebido' : enviando ? 'enviando' : envio?.estado === 'erro' ? 'erro' : podeEnviar ? 'pendente' : 'inativo'
          return (
            <li key={a.nomeArquivo} className={`documento documento-${situacao}`}>
              <span className="documento-icone" aria-hidden="true"><Icone nome="arquivo" tamanho={18} /></span>
              <div className="documento-info">
                <span className="documento-nome" title={a.nomeArquivo}>{a.nomeArquivo}</span>
                <span className="documento-detalhe">
                  {situacao === 'recebido' && (envio?.estado === 'ok' ? 'Recebido agora' : 'Recebido')}
                  {situacao === 'pendente' && 'Aguardando envio'}
                  {situacao === 'inativo' && (status === 'RECEBIDO' ? 'Envio liberado em instantes' : 'O envio não está mais disponível')}
                  {situacao === 'enviando' && 'Enviando…'}
                  {situacao === 'erro' && <span role="alert">{envio.mensagem}</span>}
                </span>
              </div>

              {situacao === 'recebido' && <span className="selo selo-sucesso"><Icone nome="check" tamanho={13} />Recebido</span>}
              {situacao === 'enviando' && <Carregando texto="" pequeno />}
              {situacao === 'inativo' && <span className="selo selo-neutro">{status === 'RECEBIDO' ? 'Em breve' : 'Não enviado'}</span>}
              {(situacao === 'pendente' || situacao === 'erro') && (podeEnviar ? (
                <>
                  <input type="file" accept="application/pdf,.pdf" hidden
                         ref={(el) => { seletores.current[a.nomeArquivo] = el }}
                         onChange={(e) => { escolherArquivo(a.nomeArquivo, e.target.files); e.target.value = '' }} />
                  <button type="button" className="botao botao-secundario botao-compacto"
                          onClick={() => seletores.current[a.nomeArquivo]?.click()}>
                    <Icone nome="upload" tamanho={15} />{situacao === 'erro' ? 'Tentar de novo' : 'Enviar PDF'}
                  </button>
                </>
              ) : <span className="selo selo-neutro">Pendente</span>)}
            </li>
          )
        })}
      </ul>

      {/* Área de arrastar e soltar: só aparece quando a API aceita envio */}
      {podeEnviar && pendentes.length > 0 && (
        <div className={`soltar${arrastando ? ' soltar-ativo' : ''}`}
             onDragOver={(e) => { e.preventDefault(); setArrastando(true) }}
             onDragLeave={() => setArrastando(false)}
             onDrop={soltar}>
          <Icone nome="upload" tamanho={20} />
          <div>
            <strong>Arraste os PDFs para cá</strong>
            <span>O nome de cada arquivo precisa ser igual ao declarado na criação.</span>
          </div>
        </div>
      )}

      {avisos.map((a, i) => (
        <div key={i} className="mensagem mensagem-erro margem" role="alert">
          <Icone nome="alerta" /><span><strong>{a.nome}</strong>: {a.mensagem}</span>
        </div>
      ))}

      {status === 'RECEBIDO' && <p className="painel-nota">O envio é liberado em alguns segundos, quando a solicitação passar para “Aguardando documentos”.</p>}
      {status === 'EXPIRADO' && pendentes.length > 0 && <p className="painel-nota">O prazo de envio terminou{prazo ? ` em ${dataHora(prazo)}` : ''}.</p>}
    </section>
  )
}
