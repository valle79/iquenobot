import { Bot, GitBranch, PlugZap, Rocket } from 'lucide-react'
import Reveal from './Reveal'
import SectionHeading from './SectionHeading'

const steps = [
  {
    icon: PlugZap,
    num: '01',
    title: 'Conecta tus canales',
    desc: 'Vincula WhatsApp, Telegram y más en minutos. Sin infraestructura, sin servidores, sin dolor de cabeza.',
  },
  {
    icon: Bot,
    num: '02',
    title: 'Entrena tu agente IA',
    desc: 'Enséñale tu negocio: productos, políticas y tono. El bot aprende de tu conocimiento y mejora solo.',
  },
  {
    icon: GitBranch,
    num: '03',
    title: 'Define las reglas',
    desc: 'Qué responde el bot, cuándo pasa a un agente, qué acciones dispara. Tú pones los límites.',
  },
  {
    icon: Rocket,
    num: '04',
    title: 'Escala sin límites',
    desc: 'Miles de conversaciones simultáneas sin contratar más gente. Tu equipo se enfoca en lo que importa.',
  },
]

export default function HowItWorks() {
  return (
    <section id="metodo" className="relative py-28 sm:py-36">
      <div className="absolute -right-40 top-1/3 h-96 w-96 rounded-full bg-cyan/8 blur-[130px]" />
      <div className="relative mx-auto max-w-7xl px-6 lg:px-10">
        <SectionHeading
          eyebrow="Cómo funciona"
          title={
            <>
              Operativo en <span className="text-gradient">menos de una semana</span>
            </>
          }
          description="Sin equipos de IT, sin meses de implementación. Cuatro pasos y tu empresa ya responde con inteligencia."
        />

        <div className="relative mt-16 grid gap-10 md:grid-cols-2 lg:grid-cols-4">
          <div className="absolute top-8 right-[12%] left-[12%] hidden h-px bg-gradient-to-r from-transparent via-brand/40 to-transparent lg:block" />
          {steps.map((s, i) => (
            <Reveal key={s.num} delay={i * 0.1} className="relative">
              <div className="group flex flex-col items-start">
                <div className="relative z-10 grid h-16 w-16 place-items-center rounded-2xl border border-brand/25 bg-base text-brand transition-all duration-300 group-hover:shadow-[0_0_40px_-10px_rgba(52,211,153,0.7)]">
                  <s.icon className="h-7 w-7" />
                  <span className="font-mono absolute -top-2 -right-2 rounded-md border border-line bg-panel px-1.5 text-[10px] text-zinc-400">
                    {s.num}
                  </span>
                </div>
                <h3 className="font-display mt-6 text-lg font-semibold text-white">{s.title}</h3>
                <p className="mt-2 text-[15px] leading-relaxed text-zinc-400">{s.desc}</p>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  )
}
