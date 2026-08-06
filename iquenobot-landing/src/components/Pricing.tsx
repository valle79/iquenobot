import { Check, Sparkles } from 'lucide-react'
import Reveal from './Reveal'
import SectionHeading from './SectionHeading'

const plans = [
  {
    name: 'Starter',
    price: '$299',
    period: '/mes',
    desc: 'Para negocios que quieren dejar de perder ventas por WhatsApp.',
    features: [
      'Hasta 2 agentes humanos',
      'Agente IA con 1.000 msgs/mes',
      'WhatsApp + Webchat',
      'Plantillas predefinidas',
      'Soporte por email',
    ],
    cta: 'Comenzar',
    featured: false,
  },
  {
    name: 'Growth',
    price: '$599',
    period: '/mes',
    desc: 'Para equipos que crecen y necesitan escala real.',
    features: [
      'Hasta 10 agentes humanos',
      'IA ilimitada y entrenada a tu negocio',
      'Todos los canales',
      'Analítica avanzada',
      'Flujos y automatizaciones',
      'Soporte prioritario 24/7',
    ],
    cta: 'Solicitar demo',
    featured: true,
  },
  {
    name: 'Enterprise',
    price: 'A medida',
    period: '',
    desc: 'Para corporaciones con requisitos de seguridad y escala.',
    features: [
      'Agentes ilimitados',
      'Despliegue en tu infraestructura',
      'SSO / SAML',
      'SLA de 99,9%',
      'Auditoría avanzada',
      'Customer success dedicado',
    ],
    cta: 'Hablar con ventas',
    featured: false,
  },
]

export default function Pricing() {
  return (
    <section id="precios" className="relative py-28 sm:py-36">
      <div className="absolute top-1/4 left-1/2 h-96 w-[700px] -translate-x-1/2 rounded-full bg-brand/8 blur-[140px]" />
      <div className="relative mx-auto max-w-7xl px-6 lg:px-10">
        <SectionHeading
          eyebrow="Precios"
          title={
            <>
              Inversión que se <span className="text-gradient">paga sola</span>
            </>
          }
          description="Cada minuto que un cliente espera una respuesta te cuesta ventas. Con IquenoBot, recuperas la inversión en semanas."
        />

        <div className="mt-16 grid gap-6 lg:grid-cols-3">
          {plans.map((p, i) => (
            <Reveal key={p.name} delay={i * 0.08}>
              <div
                className={`relative flex h-full flex-col rounded-2xl border p-8 transition-all duration-300 ${
                  p.featured
                    ? 'border-brand/40 bg-panel shadow-[0_0_60px_-20px_rgba(52,211,153,0.5)]'
                    : 'border-line bg-panel/60 hover:border-white/20'
                }`}
              >
                {p.featured && (
                  <span className="absolute -top-3.5 left-1/2 flex -translate-x-1/2 items-center gap-1.5 rounded-full bg-brand px-4 py-1.5 text-xs font-semibold text-void">
                    <Sparkles className="h-3.5 w-3.5" /> Más elegido
                  </span>
                )}
                <p className="font-mono text-xs tracking-widest text-zinc-500 uppercase">{p.name}</p>
                <div className="mt-4 flex items-end gap-1">
                  <span className="font-display text-5xl font-bold text-white">{p.price}</span>
                  {p.period && <span className="mb-1.5 text-sm text-zinc-500">{p.period}</span>}
                </div>
                <p className="mt-3 text-sm leading-relaxed text-zinc-400">{p.desc}</p>

                <ul className="mt-7 space-y-3 border-t border-line pt-7">
                  {p.features.map((f) => (
                    <li key={f} className="flex items-start gap-3 text-sm text-zinc-300">
                      <span
                        className={`mt-0.5 grid h-4.5 w-4.5 shrink-0 place-items-center rounded-full ${
                          p.featured ? 'bg-brand/15 text-brand' : 'bg-white/8 text-zinc-400'
                        }`}
                      >
                        <Check className="h-3 w-3" />
                      </span>
                      {f}
                    </li>
                  ))}
                </ul>

                <a
                  href="#cta"
                  className={`mt-8 block rounded-xl py-3.5 text-center font-semibold transition-all ${
                    p.featured
                      ? 'bg-brand text-void hover:bg-brand-soft'
                      : 'border border-line bg-white/4 text-white hover:bg-white/10'
                  }`}
                >
                  {p.cta}
                </a>
              </div>
            </Reveal>
          ))}
        </div>

        <Reveal delay={0.2} className="mt-10 text-center">
          <p className="text-sm text-zinc-500">
            ¿Tienes más de 500 conversaciones al día?{' '}
            <a href="#cta" className="font-medium text-brand underline-offset-4 hover:underline">
              Hablemos de un plan personalizado
            </a>
          </p>
        </Reveal>
      </div>
    </section>
  )
}
