import {
  Bot,
  BrainCircuit,
  Building2,
  LineChart,
  MessagesSquare,
  ShieldCheck,
  type LucideIcon,
} from 'lucide-react'
import Reveal from './Reveal'
import SectionHeading from './SectionHeading'

const features: { icon: LucideIcon; title: string; desc: string; tag?: string }[] = [
  {
    icon: Bot,
    title: 'Agente IA 24/7',
    desc: 'Responde por ti en milisegundos con lenguaje natural. Detecta intención, resuelve y solo escala al humano cuando hace falta.',
    tag: 'IA',
  },
  {
    icon: MessagesSquare,
    title: 'Bandeja omnicanal',
    desc: 'WhatsApp, Telegram, Instagram, email y webchat en una sola conversación con historial completo y contexto compartido.',
  },
  {
    icon: BrainCircuit,
    title: 'Motor de decisiones',
    desc: 'El Orchestrator decide en tiempo real: contesta el bot, asigna un agente o dispara una acción automática según la estrategia.',
    tag: 'Pipeline',
  },
  {
    icon: Building2,
    title: 'Multi-tenant seguro',
    desc: 'Aislamiento total de datos por empresa. Cada cliente es un mundo aparte, con su propia configuración y reglas.',
  },
  {
    icon: LineChart,
    title: 'Analítica en vivo',
    desc: 'Tiempos de respuesta, resolución, satisfacción y métricas por agente. Decisiones basadas en datos, no en intuición.',
  },
  {
    icon: ShieldCheck,
    title: 'Seguridad empresarial',
    desc: 'JWT rotatorio, cifrado, auditoría completa de cada acción y compliance listo para tu empresa. Deja la infraestructura en nuestras manos.',
    tag: 'SOC 2-ready',
  },
]

export default function Features() {
  return (
    <section id="producto" className="relative py-28 sm:py-36">
      <div className="absolute top-1/3 -left-40 h-96 w-96 rounded-full bg-brand/8 blur-[130px]" />
      <div className="relative mx-auto max-w-7xl px-6 lg:px-10">
        <SectionHeading
          eyebrow="El producto"
          title={
            <>
              Todo tu soporte,
              <br />
              <span className="text-gradient">una sola inteligencia</span>
            </>
          }
          description="Olvídate de saltar entre apps y rellenar tickets. IquenoBot concentra canales, IA y agentes en una plataforma que se siente como el futuro."
        />

        <div className="mt-16 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {features.map((f, i) => (
            <Reveal key={f.title} delay={i * 0.06}>
              <div className="group relative h-full overflow-hidden rounded-2xl border border-line bg-panel p-7 transition-all duration-300 hover:-translate-y-1 hover:border-brand/30 hover:shadow-[0_20px_60px_-20px_rgba(52,211,153,0.25)]">
                <div className="absolute -top-16 -right-16 h-40 w-40 rounded-full bg-brand/0 blur-3xl transition-all duration-500 group-hover:bg-brand/15" />
                <div className="flex items-start justify-between">
                  <span className="grid h-12 w-12 place-items-center rounded-xl border border-brand/20 bg-brand/10 text-brand transition-transform duration-300 group-hover:scale-110">
                    <f.icon className="h-6 w-6" />
                  </span>
                  {f.tag && (
                    <span className="rounded-full border border-line bg-white/4 px-2.5 py-1 font-mono text-[10px] tracking-widest text-zinc-500 uppercase">
                      {f.tag}
                    </span>
                  )}
                </div>
                <h3 className="font-display mt-6 text-xl font-semibold text-white">
                  {f.title}
                </h3>
                <p className="mt-2.5 text-[15px] leading-relaxed text-zinc-400">{f.desc}</p>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  )
}
