import { Star } from 'lucide-react'
import Reveal from './Reveal'
import SectionHeading from './SectionHeading'

const testimonials = [
  {
    quote:
      'Pasamos de perder ventas fuera de horario a responder a cualquier hora. El bot cierra cotizaciones mejor que muchos agentes.',
    name: 'Valeria Mendoza',
    role: 'Directora de Operaciones',
    company: 'Retail nacional',
    initial: 'V',
  },
  {
    quote:
      'La implementación fue ridículamente rápida. En una semana teníamos WhatsApp, Instagram y Telegram conectados al mismo cerebro.',
    name: 'Diego Salazar',
    role: 'CTO',
    company: 'Fintech',
    initial: 'D',
  },
  {
    quote:
      'La analítica nos abrió los ojos. Ahora sabemos exactamente dónde invertir. El ROI fue evidente desde el segundo mes.',
    name: 'Camila Rojas',
    role: 'Head of Growth',
    company: 'E-commerce',
    initial: 'C',
  },
]

export default function Testimonials() {
  return (
    <section className="relative border-y border-line bg-base/50 py-28 sm:py-32">
      <div className="relative mx-auto max-w-7xl px-6 lg:px-10">
        <SectionHeading
          eyebrow="Clientes"
          title={
            <>
              Equipos que ya viven en <span className="text-gradient">el futuro</span>
            </>
          }
        />
        <div className="mt-16 grid gap-6 md:grid-cols-3">
          {testimonials.map((t, i) => (
            <Reveal key={t.name} delay={i * 0.1}>
              <figure className="flex h-full flex-col rounded-2xl border border-line bg-panel p-7 transition-colors hover:border-white/20">
                <div className="flex gap-1 text-brand">
                  {Array.from({ length: 5 }).map((_, j) => (
                    <Star key={j} className="h-4 w-4 fill-brand" />
                  ))}
                </div>
                <blockquote className="mt-5 flex-1 text-[15px] leading-relaxed text-zinc-300">
                  “{t.quote}”
                </blockquote>
                <figcaption className="mt-7 flex items-center gap-3 border-t border-line pt-5">
                  <span className="grid h-10 w-10 place-items-center rounded-full bg-gradient-to-br from-brand to-cyan font-semibold text-void">
                    {t.initial}
                  </span>
                  <div>
                    <p className="text-sm font-semibold text-white">{t.name}</p>
                    <p className="text-xs text-zinc-500">
                      {t.role} · {t.company}
                    </p>
                  </div>
                </figcaption>
              </figure>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  )
}
