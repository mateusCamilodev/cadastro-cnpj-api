// Hooks compartilhados do portal
import { useEffect, useRef, useState } from 'react'

// Relógio: devolve Date.now() atualizado a cada "intervalo" ms.
// Use no menor componente possível — só ele redesenha a cada tique.
export function useAgora(intervalo = 1000) {
  const [agora, setAgora] = useState(Date.now())
  useEffect(() => {
    const t = setInterval(() => setAgora(Date.now()), intervalo)
    return () => clearInterval(t)
  }, [intervalo])
  return agora
}

// Foco de luz que segue o mouse. Grava a posição do ponteiro em variáveis CSS
// (--luz-x / --luz-y, relativas a cada elemento marcado com data-luz) e o CSS desenha o brilho.
// Atualiza no máximo uma vez por quadro; ao sair, remove as variáveis e a luz volta ao centro.
export function useFocoDeLuz() {
  const quadro = useRef(0)

  function mover(evento) {
    const area = evento.currentTarget
    const { clientX, clientY } = evento
    cancelAnimationFrame(quadro.current)
    quadro.current = requestAnimationFrame(() => {
      area.classList.add('com-luz')
      for (const el of [area, ...area.querySelectorAll('[data-luz]')]) {
        const r = el.getBoundingClientRect()
        el.style.setProperty('--luz-x', `${clientX - r.left}px`)
        el.style.setProperty('--luz-y', `${clientY - r.top}px`)
      }
    })
  }

  function sair(evento) {
    cancelAnimationFrame(quadro.current)
    const area = evento.currentTarget
    area.classList.remove('com-luz')
    for (const el of [area, ...area.querySelectorAll('[data-luz]')]) {
      el.style.removeProperty('--luz-x')
      el.style.removeProperty('--luz-y')
    }
  }

  useEffect(() => () => cancelAnimationFrame(quadro.current), [])
  return { onPointerMove: mover, onPointerLeave: sair }
}
