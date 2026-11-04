// Ícones de linha (SVG), no mesmo traço. Uso: <Icone nome="lista" />
const CAMINHOS = {
  painel: <><rect x="4" y="4" width="7" height="9" rx="1.5" /><rect x="13" y="4" width="7" height="5" rx="1.5" /><rect x="13" y="11" width="7" height="9" rx="1.5" /><rect x="4" y="15" width="7" height="5" rx="1.5" /></>,
  lista: <><path d="M8 6h12M8 12h12M8 18h12" /><path d="M4 6h.01M4 12h.01M4 18h.01" /></>,
  mais: <path d="M12 5v14M5 12h14" />,
  sair: <><path d="M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3" /><path d="M10 17l-5-5 5-5M5 12h11" /></>,
  check: <path d="M5 12.5l4.5 4.5L19 7.5" />,
  x: <path d="M6 6l12 12M18 6L6 18" />,
  upload: <><path d="M12 15V4M7.5 8.5L12 4l4.5 4.5" /><path d="M4 15v3a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-3" /></>,
  arquivo: <><path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" /><path d="M14 3v5h5" /></>,
  voltar: <path d="M15 18l-6-6 6-6" />,
  seta: <path d="M9 18l6-6-6-6" />,
  relogio: <><circle cx="12" cy="12" r="8.5" /><path d="M12 7.5V12l3 2" /></>,
  busca: <><circle cx="11" cy="11" r="6.5" /><path d="M20 20l-4.2-4.2" /></>,
  alerta: <><circle cx="12" cy="12" r="8.5" /><path d="M12 8v4.5M12 16h.01" /></>,
  email: <><rect x="3.5" y="5.5" width="17" height="13" rx="2" /><path d="M4 7l8 6 8-6" /></>,
  cadeado: <><rect x="5" y="10.5" width="14" height="9.5" rx="2" /><path d="M8 10.5V8a4 4 0 0 1 8 0v2.5" /></>,
  olho: <><path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z" /><circle cx="12" cy="12" r="3" /></>,
  'olho-fechado': <><path d="M3 3l18 18" /><path d="M10.6 5.6A9.6 9.6 0 0 1 12 5.5c6 0 9.5 6.5 9.5 6.5a16 16 0 0 1-3.1 3.9M6.4 6.9C3.9 8.6 2.5 12 2.5 12S6 18.5 12 18.5a9 9 0 0 0 4.3-1.1" /><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" /></>,
  copiar: <><rect x="8.5" y="8.5" width="11" height="11" rx="2" /><path d="M15.5 8.5V6a1.5 1.5 0 0 0-1.5-1.5H6A1.5 1.5 0 0 0 4.5 6v8A1.5 1.5 0 0 0 6 15.5h2.5" /></>,
  escudo: <><path d="M12 3l7.5 3v5.5c0 4.5-3.2 8.2-7.5 9.5-4.3-1.3-7.5-5-7.5-9.5V6z" /><path d="M9 12l2 2 4-4" /></>,
  play: <path d="M8 5.5v13l10.5-6.5z" />,
  download: <><path d="M12 4v11M7.5 10.5L12 15l4.5-4.5" /><path d="M4 15v3a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-3" /></>,
  refazer: <><path d="M20 12a8 8 0 1 1-2.3-5.6" /><path d="M20 4.5v4h-4" /></>,
  comparar: <><path d="M7 4v16M17 4v16" /><path d="M4 8h6M14 16h6" /></>,
  pastas: <><path d="M3.5 7.5V17a1.5 1.5 0 0 0 1.5 1.5h12" /><path d="M7 4.5h4l1.5 2H19a1.5 1.5 0 0 1 1.5 1.5v6.5A1.5 1.5 0 0 1 19 16H8.5A1.5 1.5 0 0 1 7 14.5z" /></>,
  atividade: <path d="M3 12h4l2.5-6 5 12 2.5-6H21" />,
  grafico: <><path d="M4 20h16" /><path d="M7 16v-5M12 16V7M17 16v-8" /></>,
  engrenagem: <><circle cx="12" cy="12" r="3" /><path d="M12 3.5v2.2M12 18.3v2.2M3.5 12h2.2M18.3 12h2.2M6 6l1.6 1.6M16.4 16.4L18 18M6 18l1.6-1.6M16.4 7.6L18 6" /></>,
  lixeira: <><path d="M4.5 7h15M9.5 7V4.5h5V7" /><path d="M6.5 7l1 12.5h9l1-12.5" /></>,
  lapis: <><path d="M4 20l1-4.5L15.5 5a2 2 0 0 1 3 3L8 18.5z" /><path d="M13.5 7l3 3" /></>,
  impressora: <><path d="M7 9V4h10v5" /><rect x="4" y="9" width="16" height="7" rx="1.5" /><path d="M7 14h10v6H7z" /></>,
}

export default function Icone({ nome, tamanho = 18 }) {
  return (
    <svg className="icone" width={tamanho} height={tamanho} viewBox="0 0 24 24" fill="none"
         stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {CAMINHOS[nome]}
    </svg>
  )
}
