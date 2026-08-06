import { motion } from 'framer-motion'
import { Activity, BarChart3, CheckCircle2, Inbox, Users } from 'lucide-react'
import Reveal from './Reveal'
import SectionHeading from './SectionHeading'

export default function Showcase() {
  return (
    <section id="plataforma" className="relative py-28 sm:py-36">
      <div className="grid-bg absolute inset-0 [mask-image:radial-gradient(ellipse_60%_60%_at_50%_50%,black,transparent)]" />
      <div className="relative mx-auto max-w-7xl px-6 lg:px-10">
        <SectionHeading
          eyebrow="La plataforma"
          title={
            <>
              Control total desde
              <br />
              <span className="text-gradient">una consola premium</span>
            </>
          }
          description="Un panel diseñado al detalle para que tu equipo viva adentro. Nada de ruido, solo lo que importa."
        />

        <Reveal className="mt-16">
          <div className="relative rounded-3xl border border-line bg-panel/70 p-2 shadow-[0_50px_100px_-30px_rgba(0,0,0,0.8)] backdrop-blur-xl">
            <div className="absolute -inset-px -z-10 rounded-3xl bg-gradient-to-br from-brand/30 via-transparent to-cyan/30 opacity-40 blur-xl" />
            <div className="grid gap-3 lg:grid-cols-[1fr_2.2fr]">
              <SidebarMock />
              <DashboardMock />
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  )
}

function SidebarMock() {
  const items = [
    { icon: Inbox, label: 'Bandeja', active: true, badge: '24' },
    { icon: Users, label: 'Contactos', badge: '1.2k' },
    { icon: Activity, label: 'IA y flujos' },
    { icon: BarChart3, label: 'Analítica' },
  ]
  return (
    <div className="hidden rounded-2xl border border-line bg-base p-4 lg:block">
      <p className="mb-4 px-2 text-[10px] tracking-[0.25em] text-zinc-600 uppercase">Panel</p>
      <div className="space-y-1">
        {items.map((it) => (
          <div
            key={it.label}
            className={`flex items-center justify-between rounded-lg px-3 py-2.5 text-sm ${
              it.active
                ? 'border border-brand/25 bg-brand/10 text-white'
                : 'text-zinc-400'
            }`}
          >
            <span className="flex items-center gap-2.5">
              <it.icon className={`h-4 w-4 ${it.active ? 'text-brand' : 'text-zinc-500'}`} />
              {it.label}
            </span>
            {it.badge && (
              <span
                className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                  it.active ? 'bg-brand text-void' : 'bg-white/8 text-zinc-400'
                }`}
              >
                {it.badge}
              </span>
            )}
          </div>
        ))}
      </div>
      <div className="mt-6 rounded-xl border border-line bg-white/4 p-3">
        <p className="text-xs text-zinc-500">Asistentes IA</p>
        <div className="mt-2 space-y-2">
          {['Atención', 'Ventas', 'Postventa'].map((a, i) => (
            <div key={a} className="flex items-center gap-2">
              <span className={`h-6 w-6 rounded-lg bg-gradient-to-br ${i === 0 ? 'from-brand to-cyan' : 'from-zinc-700 to-zinc-800'}`} />
              <div className="flex-1">
                <div className="flex justify-between text-xs">
                  <span className="text-zinc-300">{a}</span>
                  <span className="text-zinc-600">{100 - i * 15}%</span>
                </div>
                <div className="mt-1 h-1 overflow-hidden rounded-full bg-white/8">
                  <motion.div
                    className="h-full rounded-full bg-gradient-to-r from-brand to-cyan"
                    initial={{ width: 0 }}
                    whileInView={{ width: `${100 - i * 15}%` }}
                    viewport={{ once: true }}
                    transition={{ duration: 1.2, delay: 0.3 + i * 0.2 }}
                  />
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function DashboardMock() {
  const rows = [
    { name: 'Laura Gómez', msg: '¿Envían a Lima? 🚚', channel: 'WhatsApp', t: '2s', status: 'Resuelto', color: 'text-brand' },
    { name: 'Carlos Ríos', msg: 'Quiero cotizar 50 unidades', channel: 'Telegram', t: '1s', status: 'IA activa', color: 'text-brand' },
    { name: 'María Pérez', msg: 'Solicito cambio de talla', channel: 'Webchat', t: '04:12', status: 'Agente', color: 'text-cyan' },
    { name: 'Andrés Vega', msg: 'Factura del mes pasado', channel: 'Email', t: '11s', status: 'Resuelto', color: 'text-brand' },
  ]
  const bars = [34, 52, 41, 68, 57, 82, 74, 96, 63, 88, 71, 92]

  return (
    <div className="rounded-2xl border border-line bg-base">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-line px-5 py-4">
        <div>
          <p className="font-display text-base font-semibold text-white">Bandeja de conversaciones</p>
          <p className="text-xs text-zinc-500">Hoy · todas las agencias</p>
        </div>
        <div className="flex items-center gap-2">
          <span className="flex items-center gap-1.5 rounded-full border border-brand/25 bg-brand/10 px-3 py-1 text-xs text-brand-soft">
            <span className="h-1.5 w-1.5 rounded-full bg-brand" /> 128 en línea
          </span>
          <span className="rounded-full border border-line bg-white/4 px-3 py-1 text-xs text-zinc-400">
            Ver todos
          </span>
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full min-w-[540px] text-left text-sm">
          <thead>
            <tr className="text-[11px] tracking-widest text-zinc-600 uppercase">
              <th className="px-5 py-2.5 font-medium">Cliente</th>
              <th className="px-5 py-2.5 font-medium">Último mensaje</th>
              <th className="hidden px-5 py-2.5 font-medium md:table-cell">Canal</th>
              <th className="px-5 py-2.5 font-medium">Tiempo</th>
              <th className="px-5 py-2.5 font-medium">Estado</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r, i) => (
              <motion.tr
                key={r.name}
                initial={{ opacity: 0, x: 16 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ delay: 0.2 + i * 0.1 }}
                className="border-t border-line"
              >
                <td className="px-5 py-3">
                  <span className="flex items-center gap-2.5 font-medium text-zinc-200">
                    <span className="grid h-7 w-7 place-items-center rounded-full bg-white/8 text-xs">
                      {r.name[0]}
                    </span>
                    {r.name}
                  </span>
                </td>
                <td className="max-w-[200px] truncate px-5 py-3 text-zinc-400">{r.msg}</td>
                <td className="hidden px-5 py-3 text-zinc-400 md:table-cell">{r.channel}</td>
                <td className="px-5 py-3 font-mono text-xs text-zinc-400">{r.t}</td>
                <td className={`px-5 py-3 font-medium ${r.color}`}>
                  <span className="flex items-center gap-1.5">
                    <CheckCircle2 className="h-3.5 w-3.5" />
                    {r.status}
                  </span>
                </td>
              </motion.tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="border-t border-line px-5 py-5">
        <div className="mb-2 flex items-center justify-between">
          <p className="text-xs tracking-widest text-zinc-500 uppercase">Conversaciones por hora</p>
          <p className="font-display text-sm font-semibold text-white">
            3,214 <span className="text-xs font-normal text-brand">▲ 18%</span>
          </p>
        </div>
        <div className="flex h-20 items-end gap-1.5">
          {bars.map((b, i) => (
            <motion.div
              key={i}
              className={`flex-1 rounded-t-sm ${i % 3 === 2 ? 'bg-cyan/60' : 'bg-brand/70'}`}
              initial={{ height: 0 }}
              whileInView={{ height: `${b}%` }}
              viewport={{ once: true }}
              transition={{ duration: 0.7, delay: 0.3 + i * 0.05 }}
            />
          ))}
        </div>
      </div>
    </div>
  )
}
