import { AnimatePresence, motion } from 'framer-motion'
import { Plus } from 'lucide-react'
import { useState } from 'react'
import Reveal from './Reveal'
import SectionHeading from './SectionHeading'

const faqs = [
  {
    q: '¿Cuánto tarda en estar funcionando?',
    a: 'La mayoría de negocios están operativos en menos de una semana. Conectamos tus canales, entrenamos el agente IA con tu información y defines tus reglas. Nosotros nos encargamos de toda la infraestructura.',
  },
  {
    q: '¿Necesito conocimientos técnicos?',
    a: 'No. Todo se configura desde un panel visual. Si puedes crear una cuenta en una red social, puedes operar IquenoBot. Nuestro equipo de onboarding te acompaña en cada paso.',
  },
  {
    q: '¿Cómo funciona el agente de IA?',
    a: 'El bot responde en lenguaje natural usando el conocimiento de tu negocio que tú le proporcionas. Detecta intención y sentimiento, resuelve consultas y solo escala a un agente humano cuando detecta que hace falta.',
  },
  {
    q: '¿Qué pasa con mis datos y los de mis clientes?',
    a: 'Seguridad empresarial de serie: aislamiento multi-tenant, cifrado, JWT rotatorio, auditoría completa y soft-delete. Tus datos nunca se mezclan con los de otros clientes y tú conservas el control total.',
  },
  {
    q: '¿Puedo escalar con mi negocio?',
    a: 'Sí. IquenoBot está diseñado para pasar de 10 a 10.000 conversaciones diarias sin fricción. Cada plan crece contigo y la arquitectura soporta cientos de miles de mensajes al día.',
  },
]

export default function FAQ() {
  const [open, setOpen] = useState<number | null>(0)

  return (
    <section id="faq" className="relative py-28 sm:py-32">
      <div className="relative mx-auto max-w-3xl px-6 lg:px-10">
        <SectionHeading
          eyebrow="FAQ"
          title={
            <>
              Preguntas <span className="text-gradient">frecuentes</span>
            </>
          }
        />
        <div className="mt-14 space-y-3">
          {faqs.map((f, i) => {
            const isOpen = open === i
            return (
              <Reveal key={f.q} delay={i * 0.05}>
                <div
                  className={`overflow-hidden rounded-xl border transition-colors ${
                    isOpen ? 'border-brand/30 bg-panel' : 'border-line bg-panel/50'
                  }`}
                >
                  <button
                    onClick={() => setOpen(isOpen ? null : i)}
                    className="flex w-full items-center justify-between gap-4 px-6 py-5 text-left"
                  >
                    <span className="font-display font-semibold text-white">{f.q}</span>
                    <span
                      className={`grid h-7 w-7 shrink-0 place-items-center rounded-full border transition-all duration-300 ${
                        isOpen
                          ? 'rotate-45 border-brand/40 bg-brand/10 text-brand'
                          : 'border-line text-zinc-400'
                      }`}
                    >
                      <Plus className="h-4 w-4" />
                    </span>
                  </button>
                  <AnimatePresence initial={false}>
                    {isOpen && (
                      <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        transition={{ duration: 0.3, ease: 'easeInOut' }}
                      >
                        <p className="px-6 pb-6 text-[15px] leading-relaxed text-zinc-400">
                          {f.a}
                        </p>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>
              </Reveal>
            )
          })}
        </div>
      </div>
    </section>
  )
}
