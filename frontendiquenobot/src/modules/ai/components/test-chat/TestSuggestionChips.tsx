const SUGGESTIONS = [
  'Hola',
  '¿Qué productos venden?',
  'Quiero una cotización',
  '¿Hacen envíos?',
  '¿Qué formas de pago aceptan?',
  'Quiero hablar con un agente',
]

export function TestSuggestionChips({ onPick, disabled }: { onPick: (text: string) => void; disabled: boolean }) {
  return (
    <div className="flex flex-wrap gap-1.5 px-4 pb-3">
      {SUGGESTIONS.map((suggestion) => (
        <button
          key={suggestion}
          type="button"
          disabled={disabled}
          onClick={() => {
            onPick(suggestion)
          }}
          className="rounded-full border border-brand-200 bg-white px-3 py-1.5 text-xs font-medium text-brand-600 transition hover:bg-brand-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-brand-900 dark:bg-gray-900 dark:text-brand-400 dark:hover:bg-brand-950"
        >
          {suggestion}
        </button>
      ))}
    </div>
  )
}