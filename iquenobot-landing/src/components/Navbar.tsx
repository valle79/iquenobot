import { AnimatePresence, motion } from 'framer-motion'
import { ArrowRight, Menu, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { CRM_LOGIN_URL } from '../config'

const links = [
  { label: 'Producto', href: '#producto' },
  { label: 'Plataforma', href: '#plataforma' },
  { label: 'Método', href: '#metodo' },
  { label: 'Tecnología', href: '#tecnologia' },
  { label: 'Precios', href: '#precios' },
  { label: 'FAQ', href: '#faq' },
]

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false)
  const [open, setOpen] = useState(false)

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24)
    onScroll()
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return (
    <motion.header
      initial={{ y: -70, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ duration: 0.6, ease: 'easeOut' }}
      className={`fixed inset-x-0 top-0 z-50 transition-all duration-300 ${
        scrolled ? 'border-b border-line bg-void/80 backdrop-blur-xl' : ''
      }`}
    >
      <nav className="mx-auto flex h-16 max-w-7xl items-center justify-between px-6 lg:px-10">
        <a href="#" className="group flex items-center gap-2.5">
          <span className="glow-ring grid h-8 w-8 place-items-center rounded-lg bg-brand/10">
            <svg viewBox="0 0 64 64" className="h-5 w-5">
              <path
                d="M16 48l2.2-7A18 18 0 1 1 26 51.8L16 48z"
                fill="none"
                stroke="#34d399"
                strokeWidth="6"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <path d="M25 24c0 5 10 15 15 15" fill="none" stroke="#22d3ee" strokeWidth="6" strokeLinecap="round" />
              <circle cx="26" cy="23" r="3.5" fill="#a7f3d0" />
              <circle cx="41" cy="23" r="3.5" fill="#a7f3d0" />
            </svg>
          </span>
          <span className="font-display text-lg font-semibold tracking-tight text-white">
            Iqueno<span className="text-brand">Bot</span>
          </span>
        </a>

        <ul className="hidden items-center gap-8 lg:flex">
          {links.map((l) => (
            <li key={l.href}>
              <a
                href={l.href}
                className="text-sm text-zinc-400 transition-colors hover:text-white"
              >
                {l.label}
              </a>
            </li>
          ))}
        </ul>

        <div className="hidden items-center gap-3 lg:flex">
          <a
            href={CRM_LOGIN_URL}
            className="rounded-lg px-4 py-2 text-sm text-zinc-300 transition-colors hover:text-white"
          >
            Iniciar sesión
          </a>
          <a
            href="#cta"
            className="group inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-void transition-all hover:bg-brand-soft"
          >
            Solicitar demo
            <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
          </a>
        </div>

        <button
          className="grid h-9 w-9 place-items-center rounded-lg border border-line text-zinc-300 lg:hidden"
          onClick={() => setOpen((v) => !v)}
          aria-label="Menú"
        >
          {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
      </nav>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="overflow-hidden border-b border-line bg-void/95 backdrop-blur-xl lg:hidden"
          >
            <ul className="space-y-1 px-6 py-4">
              {links.map((l) => (
                <li key={l.href}>
                  <a
                    href={l.href}
                    onClick={() => setOpen(false)}
                    className="block rounded-lg px-3 py-2.5 text-zinc-300 transition-colors hover:bg-white/5 hover:text-white"
                  >
                    {l.label}
                  </a>
                </li>
              ))}
              <li className="pt-2">
                <a
                  href={CRM_LOGIN_URL}
                  onClick={() => setOpen(false)}
                  className="block rounded-lg px-3 py-2.5 text-zinc-300 transition-colors hover:bg-white/5 hover:text-white"
                >
                  Iniciar sesión
                </a>
              </li>
              <li className="pt-2">
                <a
                  href="#cta"
                  onClick={() => setOpen(false)}
                  className="block rounded-lg bg-brand px-3 py-2.5 text-center font-semibold text-void"
                >
                  Solicitar demo
                </a>
              </li>
            </ul>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.header>
  )
}
