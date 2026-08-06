import { Globe, Mail, MessageCircle } from 'lucide-react'

const product = ['Plataforma', 'IA', 'Precios', 'FAQ']
const company = ['Nosotros', 'Clientes', 'Contacto', 'Seguridad']

export default function Footer() {
  return (
    <footer className="border-t border-line bg-base/50">
      <div className="mx-auto max-w-7xl px-6 py-16 lg:px-10">
        <div className="grid gap-12 md:grid-cols-[1.4fr_1fr_1fr]">
          <div>
            <a href="#" className="flex items-center gap-2.5">
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
            <p className="mt-4 max-w-sm text-sm leading-relaxed text-zinc-500">
              El CRM omnicanal con IA que convierte cada mensaje de tus clientes en una
              venta resuelta.
            </p>
            <div className="mt-6 flex gap-3">
              {[MessageCircle, Globe, Mail].map((Icon, i) => (
                <a
                  key={i}
                  href="#"
                  className="grid h-9 w-9 place-items-center rounded-lg border border-line text-zinc-400 transition-colors hover:border-brand/40 hover:text-brand"
                >
                  <Icon className="h-4 w-4" />
                </a>
              ))}
            </div>
          </div>

          <div>
            <p className="font-mono text-xs tracking-widest text-zinc-500 uppercase">Producto</p>
            <ul className="mt-4 space-y-2.5">
              {product.map((l) => (
                <li key={l}>
                  <a href="#" className="text-sm text-zinc-400 transition-colors hover:text-white">
                    {l}
                  </a>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <p className="font-mono text-xs tracking-widest text-zinc-500 uppercase">Empresa</p>
            <ul className="mt-4 space-y-2.5">
              {company.map((l) => (
                <li key={l}>
                  <a href="#" className="text-sm text-zinc-400 transition-colors hover:text-white">
                    {l}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        </div>

        <div className="mt-14 flex flex-col items-center justify-between gap-4 border-t border-line pt-8 sm:flex-row">
          <p className="text-sm text-zinc-600">
            © {new Date().getFullYear()} IquenoBot. Todos los derechos reservados.
          </p>
          <div className="flex items-center gap-2 font-mono text-xs text-zinc-600">
            <span className="h-1.5 w-1.5 rounded-full bg-brand" />
            Todos los sistemas operativos
          </div>
        </div>
      </div>
    </footer>
  )
}
