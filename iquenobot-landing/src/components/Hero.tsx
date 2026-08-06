import { motion } from 'framer-motion'
import { ArrowRight, Bot, MessageSquare, Play, ShieldCheck, Sparkles, Zap } from 'lucide-react'

const stats = [
  { value: '<1s', label: 'tiempo de respuesta' },
  { value: '24/7', label: 'atención continua' },
  { value: '100%', label: 'datos aislados' },
  { value: '9+', label: 'canales soportados' },
]

export default function Hero() {
  return (
    <section className="relative overflow-hidden pt-36 pb-24 sm:pt-44">
      <div className="grid-bg absolute inset-0 [mask-image:radial-gradient(ellipse_70%_60%_at_50%_0%,black,transparent)]" />
      <div className="animate-pulse-glow absolute -top-40 left-1/2 h-[520px] w-[820px] -translate-x-1/2 rounded-full bg-brand/15 blur-[140px]" />
      <div className="absolute top-40 -right-32 h-72 w-72 rounded-full bg-cyan/10 blur-[120px]" />

      <div className="relative mx-auto max-w-7xl px-6 lg:px-10">
        <div className="mx-auto max-w-4xl text-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="inline-flex items-center gap-2 rounded-full border border-line bg-white/5 px-4 py-1.5 text-sm text-zinc-300 backdrop-blur"
          >
            <span className="relative flex h-2 w-2">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-brand opacity-75" />
              <span className="relative inline-flex h-2 w-2 rounded-full bg-brand" />
            </span>
            Nueva generación de atención al cliente
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 26 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.08 }}
            className="font-display mt-8 text-5xl leading-[1.05] font-bold tracking-tight text-white sm:text-6xl lg:text-7xl"
          >
            Tu empresa respondida
            <br />
            por <span className="text-gradient">inteligencia real</span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 26 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.16 }}
            className="mx-auto mt-6 max-w-2xl text-lg leading-relaxed text-zinc-400"
          >
            IquenoBot es el CRM omnicanal con IA que centraliza WhatsApp, Telegram y
            todos tus canales en una sola consola. Responde en milisegundos, vende más
            y escala tu equipo sin perder ni un cliente.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 26 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.24 }}
            className="mt-9 flex flex-col items-center justify-center gap-4 sm:flex-row"
          >
            <a
              href="#cta"
              className="group inline-flex w-full items-center justify-center gap-2 rounded-xl bg-brand px-7 py-3.5 text-base font-semibold text-void shadow-[0_0_40px_-10px_rgba(52,211,153,0.8)] transition-all hover:bg-brand-soft hover:shadow-[0_0_50px_-8px_rgba(52,211,153,1)] sm:w-auto"
            >
              <Sparkles className="h-5 w-5" />
              Solicitar demo gratis
              <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
            </a>
            <a
              href="#plataforma"
              className="inline-flex w-full items-center justify-center gap-2 rounded-xl border border-line bg-white/5 px-7 py-3.5 text-base font-semibold text-white backdrop-blur transition-colors hover:border-white/20 hover:bg-white/10 sm:w-auto"
            >
              <Play className="h-4 w-4" />
              Ver la plataforma
            </a>
          </motion.div>

          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.7, delay: 0.34 }}
            className="mt-5 flex items-center justify-center gap-2 text-sm text-zinc-500"
          >
            <ShieldCheck className="h-4 w-4 text-brand" />
            Sin tarjeta de crédito · Demo en 5 minutos
          </motion.p>
        </div>

        <HeroMock />

        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 0.4 }}
          className="mx-auto mt-20 grid max-w-4xl grid-cols-2 gap-8 sm:grid-cols-4"
        >
          {stats.map((s) => (
            <div key={s.value} className="text-center">
              <p className="font-display text-4xl font-bold text-white">{s.value}</p>
              <p className="mt-1 text-sm text-zinc-500">{s.label}</p>
            </div>
          ))}
        </motion.div>
      </div>
    </section>
  )
}

function HeroMock() {
  return (
    <motion.div
      initial={{ opacity: 0, y: 60, scale: 0.96 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ duration: 0.9, delay: 0.3, ease: [0.21, 0.65, 0.32, 0.99] }}
      className="relative mx-auto mt-20 max-w-5xl"
    >
      <div className="absolute -inset-x-8 -top-10 h-40 bg-gradient-to-b from-brand/10 to-transparent blur-2xl" />

      <div className="relative rounded-2xl border border-line bg-panel/80 p-2 shadow-[0_40px_80px_-20px_rgba(0,0,0,0.7)] backdrop-blur-xl sm:p-3">
        <div className="mb-2 flex items-center justify-between px-3 pt-1">
          <div className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-full bg-zinc-700" />
            <span className="h-2.5 w-2.5 rounded-full bg-zinc-700" />
            <span className="h-2.5 w-2.5 rounded-full bg-zinc-700" />
          </div>
          <div className="flex items-center gap-2 rounded-full border border-line bg-white/5 px-3 py-1 font-mono text-[10px] text-zinc-500">
            <span className="h-1.5 w-1.5 rounded-full bg-brand" />
            orchestrator · en línea
          </div>
        </div>

        <div className="grid gap-3 lg:grid-cols-[1.15fr_1fr]">
          <ChatPanel />
          <SidePanel />
        </div>
      </div>

      <div className="animate-float absolute -right-6 -top-8 hidden rounded-2xl border border-line bg-base/90 px-4 py-3 shadow-xl backdrop-blur sm:block">
        <div className="flex items-center gap-2.5">
          <span className="grid h-8 w-8 place-items-center rounded-lg bg-brand/15 text-brand">
            <Zap className="h-4 w-4" />
          </span>
          <div>
            <p className="font-display text-sm font-semibold text-white">-62% carga</p>
            <p className="text-xs text-zinc-500">en tu equipo</p>
          </div>
        </div>
      </div>

      <div className="animate-float-slow absolute -left-8 bottom-16 hidden rounded-2xl border border-line bg-base/90 px-4 py-3 shadow-xl backdrop-blur sm:block">
        <div className="flex items-center gap-2.5">
          <span className="grid h-8 w-8 place-items-center rounded-lg bg-cyan/15 text-cyan">
            <Bot className="h-4 w-4" />
          </span>
          <div>
            <p className="font-display text-sm font-semibold text-white">87%</p>
            <p className="text-xs text-zinc-500">resoluciones con IA</p>
          </div>
        </div>
      </div>
    </motion.div>
  )
}

const messages = [
  {
    from: 'client',
    text: 'Hola, ¿me pueden ayudar con mi pedido #4821? 🚚',
    time: '10:02',
  },
  { from: 'bot', text: '¡Hola! Soy el asistente de IquenoBot. Veo tu pedido #4821 y está en camino. ¿Quieres rastrearlo en vivo?', time: '10:02' },
  { from: 'client', text: 'Sí, por favor 🙏', time: '10:02' },
  { from: 'bot', text: 'Listo. Tu paquete llegará hoy entre 4 y 6 PM. ¿Te aviso cuando salga a reparto?', time: '10:03' },
  { from: 'client', text: '¡Perfecto! Muchas gracias 😊', time: '10:03' },
  { from: 'bot', text: '¡Con gusto! Te mantengo informado. ✨', time: '10:03' },
]

function ChatPanel() {
  return (
    <div className="overflow-hidden rounded-xl border border-line bg-base">
      <div className="flex items-center gap-3 border-b border-line px-4 py-3">
        <span className="grid h-9 w-9 place-items-center rounded-full bg-gradient-to-br from-brand to-cyan text-sm font-bold text-void">
          L
        </span>
        <div>
          <p className="text-sm font-semibold text-white">Laura Gómez</p>
          <p className="flex items-center gap-1 text-xs text-zinc-500">
            <span className="h-1.5 w-1.5 rounded-full bg-brand" /> atendida por IA
          </p>
        </div>
      </div>
      <div className="space-y-2.5 px-4 py-4">
        {messages.map((m, i) => (
          <div
            key={i}
            className={`flex ${m.from === 'bot' ? 'justify-end' : 'justify-start'}`}
          >
            <div
              className={`max-w-[78%] rounded-2xl px-3.5 py-2 text-[13px] leading-snug ${
                m.from === 'bot'
                  ? 'rounded-br-sm bg-brand text-void'
                  : 'rounded-bl-sm bg-white/8 text-zinc-200'
              }`}
            >
              {m.text}
              <span
                className={`mt-1 block text-right text-[10px] ${m.from === 'bot' ? 'text-void/60' : 'text-zinc-500'}`}
              >
                {m.time}
              </span>
            </div>
          </div>
        ))}
        <div className="flex justify-end">
          <div className="flex items-center gap-1 rounded-2xl rounded-br-sm bg-brand px-3.5 py-2">
            {[0, 1, 2].map((d) => (
              <motion.span
                key={d}
                className="h-1.5 w-1.5 rounded-full bg-void/60"
                animate={{ opacity: [0.3, 1, 0.3] }}
                transition={{ duration: 1.1, repeat: Infinity, delay: d * 0.18 }}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

function SidePanel() {
  return (
    <div className="hidden flex-col gap-3 lg:flex">
      <div className="rounded-xl border border-line bg-base p-4">
        <div className="flex items-center justify-between">
          <p className="text-xs tracking-widest text-zinc-500 uppercase">Conversación</p>
          <span className="rounded-full bg-brand/10 px-2 py-0.5 text-[10px] font-semibold text-brand-soft">
            RESUELTA
          </span>
        </div>
        <div className="mt-3 flex items-end gap-1.5">
          <span className="text-2xl font-bold text-white">12s</span>
          <span className="mb-1 text-xs text-zinc-500">tiempo de resolución</span>
        </div>
        <div className="mt-3 flex items-center justify-between rounded-lg bg-white/4 px-3 py-2 text-xs">
          <span className="flex items-center gap-1.5 text-zinc-400">
            <MessageSquare className="h-3.5 w-3.5 text-brand" /> Satisfacción
          </span>
          <span className="font-semibold text-brand">4.9 ★</span>
        </div>
      </div>

      <div className="flex-1 rounded-xl border border-line bg-base p-4">
        <p className="text-xs tracking-widest text-zinc-500 uppercase">Pipeline de IA</p>
        <div className="mt-3 space-y-2">
          {[
            { label: 'Intención detectada', value: 'Seguimiento pedido', w: '90%' },
            { label: 'Acción ejecutada', value: 'Enviar tracking', w: '72%' },
            { label: 'Satisfacción', value: 'Alta', w: '95%' },
          ].map((r) => (
            <div key={r.label}>
              <div className="mb-1 flex justify-between text-xs">
                <span className="text-zinc-400">{r.label}</span>
                <span className="text-zinc-300">{r.value}</span>
              </div>
              <div className="h-1.5 overflow-hidden rounded-full bg-white/8">
                <motion.div
                  className="h-full rounded-full bg-gradient-to-r from-brand to-cyan"
                  initial={{ width: 0 }}
                  animate={{ width: r.w }}
                  transition={{ duration: 1.2, delay: 0.8, ease: 'easeOut' }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
