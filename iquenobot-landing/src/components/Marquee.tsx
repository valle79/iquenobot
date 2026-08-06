import { Camera, Globe, Hash, Mail, MessageCircle, MessageSquare, Send, Smartphone } from 'lucide-react'

const channels = [
  { icon: MessageCircle, name: 'WhatsApp' },
  { icon: Send, name: 'Telegram' },
  { icon: Globe, name: 'Webchat' },
  { icon: Camera, name: 'Instagram' },
  { icon: MessageSquare, name: 'Messenger' },
  { icon: Mail, name: 'Email' },
  { icon: Smartphone, name: 'SMS' },
  { icon: Hash, name: 'X' },
]

export default function Marquee() {
  return (
    <section className="border-y border-line bg-base/50 py-10">
      <p className="mb-7 text-center font-mono text-xs tracking-[0.3em] text-zinc-500 uppercase">
        Un solo sistema · todos tus canales
      </p>
      <div className="relative overflow-hidden [mask-image:linear-gradient(to_right,transparent,black_15%,black_85%,transparent)]">
        <div className="animate-marquee flex w-max items-center gap-12 pr-12">
          {[...channels, ...channels].map((c, i) => (
            <div key={i} className="flex items-center gap-2.5 text-zinc-400">
              <c.icon className="h-5 w-5" />
              <span className="font-display text-lg font-medium whitespace-nowrap">{c.name}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
