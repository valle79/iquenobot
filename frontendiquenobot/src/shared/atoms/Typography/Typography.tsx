import { createContext, useContext, type ReactNode } from 'react'

const levelContext = createContext<number>(0)

interface TypographyProps {
  children: ReactNode
  className?: string
}

const headingStyles = ['text-4xl font-bold', 'text-3xl font-bold', 'text-2xl font-bold', 'text-xl font-semibold', 'text-lg font-semibold']

export function Heading({ children, className }: TypographyProps) {
  const level = useContext(levelContext)
  const clampedLevel = Math.min(level, headingStyles.length - 1)

  const Tag = `h${Math.min(clampedLevel + 1, 6)}` as 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6'

  return (
    <levelContext.Provider value={clampedLevel + 1}>
      <Tag className={headingStyles[clampedLevel] + ' text-gray-900 dark:text-gray-100' + (className ? ` ${className}` : '')}>
        {children}
      </Tag>
    </levelContext.Provider>
  )
}

export function Text({ children, className }: TypographyProps) {
  return <p className={'text-gray-700 dark:text-gray-300' + (className ? ` ${className}` : '')}>{children}</p>
}

export function Small({ children, className }: TypographyProps) {
  return <p className={'text-sm text-gray-500' + (className ? ` ${className}` : '')}>{children}</p>
}

export function Label({ children, className }: TypographyProps) {
  return <span className={'text-sm font-medium text-gray-700 dark:text-gray-300' + (className ? ` ${className}` : '')}>{children}</span>
}
