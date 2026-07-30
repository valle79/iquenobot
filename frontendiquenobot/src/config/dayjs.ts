import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import localizedFormat from 'dayjs/plugin/localizedFormat'
import isToday from 'dayjs/plugin/isToday'
import isYesterday from 'dayjs/plugin/isYesterday'
import calendar from 'dayjs/plugin/calendar'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'
import 'dayjs/locale/es'

// Plugins
dayjs.extend(relativeTime)
dayjs.extend(localizedFormat)
dayjs.extend(isToday)
dayjs.extend(isYesterday)
dayjs.extend(calendar)
dayjs.extend(utc)
dayjs.extend(timezone)

// Configuración global
dayjs.locale('es')
dayjs.tz.setDefault('America/Lima')

export { dayjs }