import { ArrowRight, CalendarCheck } from 'lucide-react'
import Reveal from './Reveal'

export default function CTA() {
  return (
    <section id="cta" className="relative px-6 pb-28 sm:pb-36 lg:px-10">
      <Reveal className="mx-auto max-w-6xl">
        <div className="relative overflow-hidden rounded-3xl border border-brand/25 bg-panel px-8 py-16 text-center sm:px-16 sm:py-20">
          <div className="grid-bg absolute inset-0 opacity-60 [mask-image:radial-gradient(ellipse_60%_60%_at_50%_40%,black,transparent)]" />
          <div className="animate-pulse-glow absolute -top-24 left-1/2 h-72 w-[560px] -translate-x-1/2 rounded-full bg-brand/20 blur-[120px]" />
          <div className="absolute -bottom-24 -right-24 h-64 w-64 rounded-full bg-cyan/10 blur-[100px]" />

          <div className="relative">
            <span className="inline-flex items-center gap-2 rounded-full border border-brand/25 bg-brand/10 px-4 py-1.5 font-mono text-xs tracking-widest text-brand-soft uppercase">
              <CalendarCheck className="h-3.5 w-3.5" />
              Cupos limitados este mes
            </span>
            <h2 className="font-display mx-auto mt-6 max-w-2xl text-4xl leading-tight font-bold tracking-tight text-white sm:text-5xl">
              Tu competencia ya responde.
              <br />
              <span className="text-gradient">¿Y tú?</span>
            </h2>
            <p className="mx-auto mt-5 max-w-xl text-lg text-zinc-400">
              Agenda una demo de 20 minutos y mira a IquenoBot atender clientes en vivo,
              con tus productos. Sin compromiso.
            </p>
            <div className="mt-9 flex flex-col items-center justify-center gap-4 sm:flex-row">
              <a
                href="mailto:ventas@iquenobot.com"
                className="group inline-flex w-full items-center justify-center gap-2 rounded-xl bg-brand px-8 py-4 text-base font-semibold text-void shadow-[0_0_50px_-10px_rgba(52,211,153,0.9)] transition-all hover:bg-brand-soft sm:w-auto"
              >
                Agendar mi demo gratis
                <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
              </a>
              <a
                href="mailto:soporte@iquenobot.com"
                className="inline-flex w-full items-center justify-center rounded-xl border border-line bg-white/5 px-8 py-4 text-base font-semibold text-white backdrop-blur transition-colors hover:bg-white/10 sm:w-auto"
              >
                Hablar con ventas
              </a>
            </div>
            <p className="mt-6 text-sm text-zinc-500">
              Demo personalizada con tu catálogo · Activación en menos de una semana · Sin permanencia
            </p>
          </div>
        </div>
      </Reveal>
    </section>
  )
}
