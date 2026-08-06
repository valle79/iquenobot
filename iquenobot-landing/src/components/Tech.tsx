import { motion } from 'framer-motion'
import Reveal from './Reveal'

const stack = [
  'Java 21',
  'Spring Boot 3.4',
  'PostgreSQL 15',
  'React 19',
  'TypeScript',
  'OpenAI',
  'Evolution API',
  'Docker',
  'Kubernetes-ready',
  'Flyway',
  'Redis',
  'JWT + OAuth2',
]

export default function Tech() {
  return (
    <section id="tecnologia" className="relative border-y border-line bg-base/50 py-28 sm:py-32">
      <div className="relative mx-auto max-w-7xl px-6 lg:px-10">
        <div className="grid items-center gap-14 lg:grid-cols-2">
          <Reveal>
            <span className="inline-flex items-center gap-2 rounded-full border border-cyan/25 bg-cyan/10 px-4 py-1.5 font-mono text-xs tracking-widest text-cyan uppercase">
              <span className="h-1.5 w-1.5 rounded-full bg-cyan" />
              Ingeniería
            </span>
            <h2 className="font-display mt-6 text-4xl font-semibold tracking-tight text-white sm:text-5xl">
              Arquitectura de <span className="text-gradient">nivel enterprise</span>
            </h2>
            <p className="mt-5 text-lg leading-relaxed text-zinc-400">
              Clean Architecture, Domain-Driven Design y Multi-Tenancy real. Cada empresa
              con aislamiento completo de datos, auditoría de cada acción y un pipeline de
              IA desacoplado que crece contigo.
            </p>
            <ul className="mt-8 space-y-3">
              {[
                'Clean Architecture + DDD',
                'Multi-tenant con aislamiento total',
                'Proveedores de IA y WhatsApp intercambiables',
                'Métricas en tiempo real con Micrometer',
              ].map((t) => (
                <li key={t} className="flex items-center gap-3 text-zinc-300">
                  <span className="h-1.5 w-1.5 rounded-full bg-brand" />
                  {t}
                </li>
              ))}
            </ul>
          </Reveal>

          <Reveal delay={0.15}>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
              {stack.map((s, i) => (
                <motion.div
                  key={s}
                  whileHover={{ y: -4 }}
                  className="flex items-center gap-2.5 rounded-xl border border-line bg-panel px-4 py-3.5 text-sm text-zinc-300 transition-colors hover:border-cyan/30"
                >
                  <span className="h-1.5 w-1.5 rounded-full bg-gradient-to-r from-brand to-cyan" />
                  <span className="font-mono text-[13px]">{s}</span>
                  <span className="ml-auto font-mono text-[10px] text-zinc-600">{String(i + 1).padStart(2, '0')}</span>
                </motion.div>
              ))}
            </div>
          </Reveal>
        </div>
      </div>
    </section>
  )
}
