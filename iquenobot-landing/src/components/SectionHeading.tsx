import Reveal from './Reveal'

export default function SectionHeading({
  eyebrow,
  title,
  description,
}: {
  eyebrow: string
  title: React.ReactNode
  description?: string
}) {
  return (
    <Reveal className="mx-auto max-w-3xl text-center">
      <span className="inline-flex items-center gap-2 rounded-full border border-brand/25 bg-brand/10 px-4 py-1.5 font-mono text-xs tracking-widest text-brand-soft uppercase">
        <span className="h-1.5 w-1.5 rounded-full bg-brand" />
        {eyebrow}
      </span>
      <h2 className="font-display mt-6 text-4xl font-semibold tracking-tight text-white sm:text-5xl">
        {title}
      </h2>
      {description && (
        <p className="mt-5 text-lg leading-relaxed text-zinc-400">{description}</p>
      )}
    </Reveal>
  )
}
