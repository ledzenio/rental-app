import { useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent, ReactElement } from 'react'
import { Link, NavLink, Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom'
import { api } from './api'
import { useAuthStore, useRoles } from './authStore'
import { notifyError, notifyInfo, notifySuccess, notifyWarning } from './noticeStore'
import axios from 'axios'
import * as XLSX from 'xlsx'
import type {
  AdminUser,
  AuthPayload,
  AnalyticsReport,
  DefectReport,
  DefectReportStatus,
  DefectSeverity,
  EquipmentRepairAlert,
  EquipmentState,
  GeocodingEstimate,
  Invoice,
  ManagerServiceRequest,
  Paged,
  Profile,
  RevenueReport,
  Role,
  SavedService,
  ServiceItem,
  ServiceRequest,
  ServiceRequestStatus,
  SpecialistEquipmentOverview,
  SpecialistServiceRequestOption,
  EquipmentStateTimelineEntry,
  UserStatus,
} from './types'
import { Bar, BarChart, CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

const YANDEX_MAPS_API_KEY = import.meta.env.VITE_YANDEX_MAPS_API_KEY ?? ''
let yandexMapsLoader: Promise<void> | null = null

type IconProps = { className?: string }

function IconCatalog({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path d="M4 4h7v7H4V4Zm9 0h7v7h-7V4Zm-9 9h7v7H4v-7Zm9 3h7v4h-7v-4Z" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  )
}

function IconBookmark({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path d="M7 4h10a1 1 0 0 1 1 1v15l-6-3.5L6 20V5a1 1 0 0 1 1-1Z" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  )
}

function IconRequests({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path d="M7 6h10M7 12h10M7 18h6M5 4h14a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1Z" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

function IconInvoices({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path d="M6 3h12v18l-2-1.5L14 21l-2-1.5L10 21l-2-1.5L6 21V3Z" stroke="currentColor" strokeWidth="1.8" />
      <path d="M9 8h6M9 12h6M9 16h4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

function IconUser({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <circle cx="12" cy="8" r="3.2" stroke="currentColor" strokeWidth="1.8" />
      <path d="M5 19a7 7 0 0 1 14 0" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

function IconManager({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <rect x="4" y="5" width="16" height="14" rx="2" stroke="currentColor" strokeWidth="1.8" />
      <path d="M8 9h8M8 13h8M8 17h5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

function IconTools({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path d="M14 6.5a4.5 4.5 0 0 0-5.8 5.8L3.8 16.7a1.5 1.5 0 0 0 2.1 2.1l4.4-4.4A4.5 4.5 0 0 0 16 8.6l-2 2-1.7-1.7 1.7-2.4Z" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function IconLogout({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path d="M10 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      <path d="M14 16l4-4-4-4M18 12H9" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function IconPlus({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path d="M12 5v14M5 12h14" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

function IconEdit({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <path d="M4 20h4l10-10-4-4L4 16v4Z" stroke="currentColor" strokeWidth="1.8" />
      <path d="m12 6 4 4" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  )
}

function IconLock({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden>
      <rect x="5" y="11" width="14" height="9" rx="2" stroke="currentColor" strokeWidth="1.8" />
      <path d="M8 11V8a4 4 0 0 1 8 0v3" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  )
}

function GoogleIcon({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 18 18" aria-hidden>
      <path d="M17.64 9.2c0-.64-.06-1.25-.16-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.71v2.25h2.9c1.7-1.56 2.7-3.85 2.7-6.6Z" fill="#4285F4" />
      <path d="M9 18c2.43 0 4.47-.8 5.96-2.2l-2.9-2.25c-.8.54-1.84.86-3.06.86-2.35 0-4.34-1.59-5.06-3.72H.96v2.33A9 9 0 0 0 9 18Z" fill="#34A853" />
      <path d="M3.94 10.69A5.41 5.41 0 0 1 3.66 9c0-.59.1-1.16.28-1.69V4.98H.96A9 9 0 0 0 0 9c0 1.45.35 2.82.96 4.02l2.98-2.33Z" fill="#FBBC05" />
      <path d="M9 3.58c1.32 0 2.5.45 3.44 1.34l2.58-2.58A8.98 8.98 0 0 0 9 0 9 9 0 0 0 .96 4.98L3.94 7.3c.72-2.13 2.71-3.72 5.06-3.72Z" fill="#EA4335" />
    </svg>
  )
}

function ensureYandexMapsLoaded() {
  if (typeof window === 'undefined') return Promise.reject(new Error('Window is not available'))
  if ((window as any).ymaps?.Map) return Promise.resolve()
  if (yandexMapsLoader) return yandexMapsLoader
  yandexMapsLoader = new Promise<void>((resolve, reject) => {
    const script = document.createElement('script')
    const query = new URLSearchParams({
      lang: 'ru_RU',
      apikey: YANDEX_MAPS_API_KEY,
    })
    script.src = `https://api-maps.yandex.ru/2.1/?${query.toString()}`
    script.async = true
    script.onload = () => {
      const ymaps = (window as any).ymaps
      if (!ymaps?.ready) {
        reject(new Error('Yandex Maps API failed to initialize'))
        return
      }
      ymaps.ready(() => resolve())
    }
    script.onerror = () => reject(new Error('Failed to load Yandex Maps script'))
    document.head.appendChild(script)
  })
  return yandexMapsLoader
}

function Protected({ children }: { children: ReactElement }) {
  const token = useAuthStore((s) => s.accessToken)
  if (!token) return <Navigate to="/auth" replace />
  return children
}

function RoleProtected({ roles, children }: { roles: Role[]; children: ReactElement }) {
  const userRole = useAuthStore((s) => s.role)
  const allowed = userRole ? roles.includes(userRole) : false
  if (!allowed) return <Navigate to="/services" replace />
  return children
}

function EmptyState({ text }: { text: string }) {
  return <p className="muted">{text}</p>
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('ru-RU')
}

function formatInvoiceLocalDate(iso: string | null | undefined): string {
  if (!iso) return ''
  const parts = iso.split('-').map(Number)
  if (parts.length !== 3 || parts.some((n) => Number.isNaN(n))) return iso
  const [y, m, d] = parts
  return new Date(y, m - 1, d).toLocaleDateString('ru-RU')
}

function invoiceDisplayStatus(status: string): { label: string; cls: string } {
  const map: Record<string, { label: string; cls: string }> = {
    ISSUED: { label: 'К оплате', cls: 'status-payment' },
    PAID: { label: 'Оплачен', cls: 'status-completed' },
    CANCELLED: { label: 'Отменён', cls: 'status-cancelled' },
  }
  return map[status] ?? { label: status, cls: 'status-new' }
}

function invoiceCurrencyLabel(code: string): string {
  return code === 'RUB' ? 'BYN' : code
}

function userInvoicePrimaryTitle(x: Invoice): string {
  if (x.invoiceType === 'SERVICE') {
    const t = x.serviceTitle?.trim()
    if (t) return t
    if (x.serviceRequestId != null) return `Заявка №${x.serviceRequestId}`
    return 'Услуга'
  }
  if (x.invoiceType === 'PENALTY') return 'Штраф'
  return x.description?.trim() ? x.description.trim().slice(0, 120) : `Счёт №${x.id}`
}

function userInvoiceSubtitleLines(x: Invoice): string[] {
  const lines: string[] = []
  if (x.invoiceType === 'SERVICE') {
    const parts: string[] = []
    const titleIsService = Boolean(x.serviceTitle?.trim())
    if (titleIsService && x.serviceRequestId != null) {
      parts.push(`Заявка №${x.serviceRequestId}`)
    }
    if (x.rentalStartDate && x.rentalEndDate) {
      parts.push(
        `Период аренды: ${formatInvoiceLocalDate(x.rentalStartDate)} — ${formatInvoiceLocalDate(x.rentalEndDate)}`,
      )
    }
    if (parts.length) lines.push(parts.join(' · '))
  }
  if (x.invoiceType === 'PENALTY') {
    if (x.defectReportId != null) lines.push(`Дефектная ведомость №${x.defectReportId}`)
  }
  const desc = x.description?.trim()
  if (desc) lines.push(desc)
  return lines
}

const BY_OPERATOR_CODES = new Set(['17', '25', '29', '33', '44'])

/** После кода страны 375: 2 цифры оператора + 7 цифр абонента (макс. 9). */
function belarusNationalDigitsFromInput(raw: string): string {
  const all = raw.replace(/\D/g, '')
  const withoutCountry = all.startsWith('375') ? all.slice(3) : all
  return withoutCountry.slice(0, 9)
}

/** Отображение с фиксированным «+375 » и авто‑скобками/пробелами/дефисами; пользователь вводит только цифры. */
function formatBelarusPhoneMasked(nationalDigits: string): string {
  const d = nationalDigits.replace(/\D/g, '').slice(0, 9)
  const prefix = '+375 '
  if (d.length === 0) return prefix
  if (d.length === 1) return `${prefix}(${d}`
  if (d.length === 2) return `${prefix}(${d}) `
  const op = d.slice(0, 2)
  const rest = d.slice(2)
  let s = `${prefix}(${op})`
  if (rest.length === 0) return `${s} `
  s += ` ${rest.slice(0, 3)}`
  if (rest.length <= 3) return s
  s += `-${rest.slice(3, 5)}`
  if (rest.length <= 5) return s
  return `${s}-${rest.slice(5)}`
}

/** Канон для API: '+375 (44) 123-45-67'; null если пусто или неполный/неверный код оператора. */
function canonicalBelarusPhoneNational(national9: string): string | null {
  const d = national9.replace(/\D/g, '').slice(0, 9)
  if (d.length === 0) return null
  if (d.length !== 9) return null
  const op = d.slice(0, 2)
  if (!BY_OPERATOR_CODES.has(op)) return null
  const r = d.slice(2)
  return `+375 (${op}) ${r.slice(0, 3)}-${r.slice(3, 5)}-${r.slice(5)}`
}

function backendOrigin() {
  const base = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8088/api/v1'
  return base.replace(/\/api\/v1\/?$/, '')
}

function parseIsoDate(value: string): Date | null {
  if (!value) return null
  const parsed = new Date(`${value}T00:00:00`)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

const DRAFT_KEY = 'serviceRequestDraftByService'

function statusMeta(status: ServiceRequestStatus) {
  const map: Record<ServiceRequestStatus, { label: string; cls: string }> = {
    NEW: { label: 'Новая', cls: 'status-new' },
    AWAITING_PAYMENT: { label: 'Ожидает оплаты', cls: 'status-payment' },
    IN_PROGRESS: { label: 'В работе', cls: 'status-progress' },
    AWAITING_SPECIALIST_REVIEW: { label: 'Ожидает заключения специалиста', cls: 'status-feedback' },
    COMPLETED: { label: 'Завершена', cls: 'status-completed' },
    CANCELLED: { label: 'Отменена', cls: 'status-cancelled' },
  }
  return map[status]
}

function defectStatusMeta(status: DefectReportStatus) {
  const map: Record<DefectReportStatus, { label: string; cls: string }> = {
    NEW: { label: 'Новая', cls: 'status-new' },
    SENT_TO_MANAGER: { label: 'Передана менеджеру', cls: 'status-feedback' },
    APPROVED: { label: 'Утверждена', cls: 'status-completed' },
    REJECTED: { label: 'Отклонена', cls: 'status-cancelled' },
  }
  return map[status]
}

const MANAGER_DEFECT_SUMMARY_LABELS: Record<string, string> = {
  new: 'Новые',
  sentToManager: 'Переданы менеджеру',
  approved: 'Утверждены',
  rejected: 'Отклонены',
}

function defectSeverityRu(severity: DefectSeverity): string {
  const map: Record<DefectSeverity, string> = {
    LOW: 'Низкая',
    MEDIUM: 'Средняя',
    HIGH: 'Высокая',
    CRITICAL: 'Критическая',
  }
  return map[severity]
}

function defectSeverityChipClass(severity: DefectSeverity): string {
  const map: Record<DefectSeverity, string> = {
    LOW: 'status-completed',
    MEDIUM: 'status-feedback',
    HIGH: 'status-payment',
    CRITICAL: 'status-cancelled',
  }
  return map[severity]
}

function defectPaymentStatusMeta(status: string): { label: string; cls: string } {
  const map: Record<string, { label: string; cls: string }> = {
    NOT_ISSUED: { label: 'Счет не выставлен', cls: 'status-new' },
    ISSUED: { label: 'Ожидает оплаты', cls: 'status-payment' },
    PAID: { label: 'Оплачена', cls: 'status-completed' },
    CANCELLED: { label: 'Счет отменен', cls: 'status-cancelled' },
  }
  return map[status] ?? { label: status, cls: 'status-new' }
}

/** После утверждения: нулевая сумма означает, что счёт не создавался намеренно (в т.ч. «дефекты не выявлены»). */
function managerDefectPenaltyPaymentMeta(x: DefectReport): { label: string; cls: string } {
  if (x.status === 'APPROVED' && x.penaltyInvoiceStatus === 'NOT_ISSUED' && Number(x.recommendedPenalty) <= 0) {
    return { label: 'Оплата не требуется', cls: 'status-completed' }
  }
  return defectPaymentStatusMeta(x.penaltyInvoiceStatus)
}

function formatServiceRequestStatus(value: string) {
  const knownStatuses: ServiceRequestStatus[] = ['NEW', 'AWAITING_PAYMENT', 'IN_PROGRESS', 'AWAITING_SPECIALIST_REVIEW', 'COMPLETED', 'CANCELLED']
  if (knownStatuses.includes(value as ServiceRequestStatus)) {
    return statusMeta(value as ServiceRequestStatus)
  }
  return { label: value, cls: '' }
}

function ClickableMap({
  selectedPoint,
  onSelect,
}: {
  selectedPoint: { lat: number; lng: number } | null
  onSelect: (lat: number, lng: number) => void
}) {
  const mapRef = useRef<HTMLDivElement | null>(null)
  const yMapRef = useRef<any>(null)
  const markerRef = useRef<any>(null)
  const [mapError, setMapError] = useState<string | null>(null)

  useEffect(() => {
    if (!mapRef.current || yMapRef.current) return
    if (!YANDEX_MAPS_API_KEY) {
      const msg = 'Не задан ключ Яндекс.Карт (VITE_YANDEX_MAPS_API_KEY).'
      notifyError(msg)
      setMapError(msg)
      return
    }
    void ensureYandexMapsLoaded()
      .then(() => {
        const ymaps = (window as any).ymaps
        if (!ymaps?.Map) {
          const msg = 'Не удалось инициализировать Яндекс.Карты.'
          notifyError(msg)
          setMapError(msg)
          return
        }
        const map = new ymaps.Map(mapRef.current, {
          center: [53.9023, 27.5619],
          zoom: 11,
          controls: ['zoomControl'],
        })
        map.events.add('click', (event: any) => {
          const coords = event.get('coords') as [number, number]
          if (!coords || coords.length !== 2) return
          onSelect(coords[0], coords[1])
        })
        yMapRef.current = map
      })
      .catch(() => {
        const msg = 'Не удалось загрузить Яндекс.Карты.'
        notifyError(msg)
        setMapError(msg)
      })
    return () => {
      if (yMapRef.current) {
        yMapRef.current.destroy()
        yMapRef.current = null
      }
      markerRef.current = null
    }
  }, [onSelect])

  useEffect(() => {
    const map = yMapRef.current
    if (!map) return
    const ymaps = (window as any).ymaps
    if (!ymaps?.Placemark) return
    if (!selectedPoint) {
      if (markerRef.current) {
        map.geoObjects.remove(markerRef.current)
        markerRef.current = null
      }
      return
    }
    if (!markerRef.current) {
      markerRef.current = new ymaps.Placemark([selectedPoint.lat, selectedPoint.lng], {}, { preset: 'islands#redDotIcon' })
      map.geoObjects.add(markerRef.current)
    } else {
      markerRef.current.geometry.setCoordinates([selectedPoint.lat, selectedPoint.lng])
    }
    map.panTo([selectedPoint.lat, selectedPoint.lng], { flying: true })
  }, [selectedPoint])

  if (mapError) {
    return (
      <div className="inline-callout inline-callout--error">
        <strong>Карта недоступна</strong>
        <p>{mapError}</p>
      </div>
    )
  }
  return <div ref={mapRef} style={{ height: 220, width: '100%' }} />
}

function Header() {
  const { isUser, isManager, isSpecialist } = useRoles()
  const token = useAuthStore((s) => s.accessToken)
  const virtualBalance = useAuthStore((s) => s.virtualBalance)
  const setBalance = useAuthStore((s) => s.setBalance)
  const clear = useAuthStore((s) => s.clear)
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    if (!token) {
      setBalance(null)
      return
    }
    let active = true
    const loadBalance = async () => {
      try {
        const { data } = await api.get<Profile>('/auth/me')
        if (active) setBalance(data.virtualBalance)
      } catch {
        // Ignore header balance fetch errors to avoid noisy notifications.
      }
    }
    void loadBalance()
    return () => {
      active = false
    }
  }, [location.pathname, setBalance, token])

  return (
    <header className="top shell-block">
      <div className="brand">
        <h1>Rental Service</h1>
        <p className="muted-light">Аренда, обслуживание и контроль техники</p>
      </div>
      <nav className="main-nav">
        <NavLink to="/services"><span className="nav-link-inner"><IconCatalog className="ui-icon" />Каталог</span></NavLink>
        {isUser ? <NavLink to="/saved"><span className="nav-link-inner"><IconBookmark className="ui-icon" />Отложенные</span></NavLink> : null}
        {isUser ? <NavLink to="/requests"><span className="nav-link-inner"><IconRequests className="ui-icon" />Заявки</span></NavLink> : null}
        {isUser ? <NavLink to="/invoices"><span className="nav-link-inner"><IconInvoices className="ui-icon" />Счета</span></NavLink> : null}
        <NavLink to="/profile"><span className="nav-link-inner"><IconUser className="ui-icon" />Профиль</span></NavLink>
        {isManager ? <NavLink to="/manager"><span className="nav-link-inner"><IconManager className="ui-icon" />Менеджер</span></NavLink> : null}
        {isSpecialist ? <NavLink to="/specialist"><span className="nav-link-inner"><IconTools className="ui-icon" />Специалист</span></NavLink> : null}
      </nav>
      <div className="user-pane">
        {token ? (
          <>
            <div className="balance-chip">
              <span className="balance-chip__label">Баланс</span>
              <strong>{virtualBalance !== null ? `${virtualBalance.toFixed(2)} BYN` : '... BYN'}</strong>
            </div>
            <button
              className="ghost"
              onClick={() => {
                clear()
                navigate('/auth')
              }}
            >
              <span className="btn-with-icon"><IconLogout className="ui-icon" />Выйти</span>
            </button>
          </>
        ) : (
          <Link className="btn-link" to="/auth"><span className="btn-with-icon"><IconLogout className="ui-icon" />Войти</span></Link>
        )}
      </div>
    </header>
  )
}

function AuthPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const setAuth = useAuthStore((s) => s.setAuth)
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fullName, setFullName] = useState('')
  const [loading, setLoading] = useState(false)
  const oauth2Error = new URLSearchParams(location.search).get('oauth2Error')
  const oauthNoticeShownRef = useRef(false)

  useEffect(() => {
    if (!oauth2Error) {
      oauthNoticeShownRef.current = false
      return
    }
    if (oauthNoticeShownRef.current) return
    oauthNoticeShownRef.current = true
    notifyWarning(`Вход через Google не выполнен (${oauth2Error}). Попробуйте снова или войдите по email и паролю.`)
  }, [oauth2Error])

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setLoading(true)
    try {
      const path = mode === 'login' ? '/auth/login' : '/auth/register'
      const body = mode === 'login' ? { email, password } : { email, password, fullName }
      const { data } = await api.post<AuthPayload>(path, body)
      setAuth(data)
      navigate('/services')
    } catch (e) {
      notifyError('Не удалось выполнить авторизацию. Проверьте email и пароль.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="card auth-shell shell-block">
      <div className="auth-visual">
        <span className="auth-badge">Tech Rental</span>
        <h2>Арендуйте технику за минуты</h2>
        <p>
          Каталог с актуальной доступностью, прозрачные счета и контроль состояния техники
          в одном удобном кабинете.
        </p>
        <ul className="auth-benefits">
          <li>Мгновенная регистрация и вход</li>
          <li>Баланс и счета всегда под рукой</li>
          <li>Заявки и статусы в реальном времени</li>
        </ul>
      </div>
      <div className="auth-panel">
        <h3>{mode === 'login' ? 'Вход в систему' : 'Создание аккаунта'}</h3>
        <p className="muted-light">
          {mode === 'login'
            ? 'Введите email и пароль, чтобы перейти к аренде.'
            : 'Заполните данные и получите доступ к сервису аренды техники.'}
        </p>
        <div className="segmented auth-segmented">
          <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>
            Вход
          </button>
          <button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>
            Регистрация
          </button>
        </div>
        <form className="form-grid auth-form" onSubmit={onSubmit}>
          <label>
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="name@example.com" required />
          </label>
          <label>
            Пароль
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>
          {mode === 'register' ? (
            <label>
              ФИО
              <input value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Иванов Иван Иванович" required />
            </label>
          ) : null}
          <button disabled={loading} type="submit">
            {loading ? 'Отправка...' : mode === 'login' ? 'Войти в кабинет' : 'Создать аккаунт'}
          </button>
        </form>
        <button
          type="button"
          className="oauth-btn"
          onClick={() => {
            window.location.href = `${backendOrigin()}/oauth2/authorization/google`
          }}
        >
          <GoogleIcon className="google-icon" />
          Continue with Google
        </button>
      </div>
    </section>
  )
}

function OAuthCallbackPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const setAuth = useAuthStore((s) => s.setAuth)

  useEffect(() => {
    const params = new URLSearchParams(location.search)
    const accessToken = params.get('accessToken')
    const refreshToken = params.get('refreshToken')
    const email = params.get('email')
    const roleRaw = params.get('role')
    if (!accessToken || !refreshToken || !email || !roleRaw) {
      navigate('/auth?oauth2Error=invalid_callback', { replace: true })
      return
    }
    setAuth({
      accessToken,
      refreshToken,
      tokenType: 'Bearer',
      email,
      role: roleRaw as Role,
    })
    navigate('/', { replace: true })
  }, [location.search, navigate, setAuth])

  return <section className="card"><p className="muted">Завершаем вход через Google...</p></section>
}

function ServicesPage() {
  const [query, setQuery] = useState('')
  const [selectedCategories, setSelectedCategories] = useState<string[]>([])
  const [sortBy, setSortBy] = useState<'title_asc' | 'title_desc' | 'price_asc' | 'price_desc'>('title_asc')
  const [items, setItems] = useState<ServiceItem[]>([])
  const [loading, setLoading] = useState(false)
  const categoryOptions = useMemo(
    () => Array.from(new Set(items.map((item) => item.category || 'Прочее'))).sort((a, b) => a.localeCompare(b, 'ru')),
    [items],
  )
  const filteredSorted = useMemo(() => {
    const search = query.trim().toLowerCase()
    const filtered = items.filter((item) => {
      const categoryKey = item.category || 'Прочее'
      const matchCategory = selectedCategories.length === 0 || selectedCategories.includes(categoryKey)
      if (!matchCategory) return false
      if (!search) return true
      const haystack = `${item.title} ${item.subtitle ?? ''} ${item.description}`.toLowerCase()
      return haystack.includes(search)
    })
    return filtered.sort((a, b) => {
      if (sortBy === 'price_asc') return a.basePrice - b.basePrice
      if (sortBy === 'price_desc') return b.basePrice - a.basePrice
      if (sortBy === 'title_desc') return b.title.localeCompare(a.title, 'ru')
      return a.title.localeCompare(b.title, 'ru')
    })
  }, [items, query, selectedCategories, sortBy])
  const grouped = useMemo(() => {
    const map = new Map<string, ServiceItem[]>()
    filteredSorted.forEach((item) => {
      const key = item.category || 'Прочее'
      const list = map.get(key) ?? []
      list.push(item)
      map.set(key, list)
    })
    return Array.from(map.entries())
  }, [filteredSorted])

  async function load() {
    setLoading(true)
    const { data } = await api.get<Paged<ServiceItem>>('/services', {
      params: { active: true, size: 120, sort: 'id,desc' },
    })
    const normalized = (data.content ?? []).map((item) => ({
      ...item,
      specs: item.specs ?? [],
      images: item.images ?? [],
      coverImageUrl: item.coverImageUrl ?? null,
      subtitle: item.subtitle ?? null,
    }))
    setItems(normalized)
    setLoading(false)
  }

  useEffect(() => {
    void load()
  }, [])

  function toggleCategory(category: string) {
    setSelectedCategories((prev) => (
      prev.includes(category)
        ? prev.filter((x) => x !== category)
        : [...prev, category]
    ))
  }

  return (
    <section className="card page-section">
      <div className="page-section-head">
        <div>
          <h2>Каталог услуг</h2>
          <p className="muted">Выберите категорию и найдите подходящую технику для аренды.</p>
        </div>
      </div>
      <div className="toolbar page-toolbar">
        <input placeholder="Поиск по названию или описанию" value={query} onChange={(e) => setQuery(e.target.value)} />
        <select value={sortBy} onChange={(e) => setSortBy(e.target.value as typeof sortBy)}>
          <option value="title_asc">Сортировка: название А-Я</option>
          <option value="title_desc">Сортировка: название Я-А</option>
          <option value="price_asc">Сортировка: цена по возрастанию</option>
          <option value="price_desc">Сортировка: цена по убыванию</option>
        </select>
        <button onClick={load}>Обновить каталог</button>
        {selectedCategories.length > 0 ? (
          <button type="button" className="ghost" onClick={() => setSelectedCategories([])}>
            Сбросить категории
          </button>
        ) : null}
      </div>
      <div className="multi-category-filter">
        {categoryOptions.length ? categoryOptions.map((category) => (
          <label key={category} className="checkbox category-check">
            <input
              type="checkbox"
              checked={selectedCategories.includes(category)}
              onChange={() => toggleCategory(category)}
            />
            <span>{category}</span>
          </label>
        )) : (
          <span className="muted">Категории появятся после загрузки каталога.</span>
        )}
      </div>
      {loading ? <p className="muted">Загрузка...</p> : null}
      {grouped.map(([group, groupItems]) => (
        <section key={group} className="category-section section-block">
          <h3>{group}</h3>
          <div className="services-grid">
            {groupItems.map((item) => (
              <article key={item.id} className="service-card catalog-card">
                <img
                  className="service-cover"
                  src={item.coverImageUrl ?? 'https://placehold.co/800x450?text=Rental+Service'}
                  alt={item.title}
                />
                <div className="service-card-body">
                  <h4>{item.title}</h4>
                  {item.subtitle ? <p className="muted line-clamp">{item.subtitle}</p> : null}
                  <ul className="spec-list">
                    {(item.specs ?? []).slice(0, 3).map((spec) => (
                      <li key={`${item.id}-${spec.key}`}>
                        <span className="muted">{spec.key}:</span> <strong>{spec.value}</strong>
                      </li>
                    ))}
                  </ul>
                  <div className="row">
                    <Link className="btn-link details-btn" to={`/services/${item.id}`}>
                      Подробнее
                    </Link>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </section>
      ))}
    </section>
  )
}

function ServiceDetailsPage() {
  const { id } = useParams()
  const [item, setItem] = useState<ServiceItem | null>(null)
  const [loading, setLoading] = useState(false)
  const [loadFailed, setLoadFailed] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const [carouselIndex, setCarouselIndex] = useState(0)
  const [requestNotes, setRequestNotes] = useState('')
  const [selectedPoint, setSelectedPoint] = useState<{ lat: number; lng: number } | null>(null)
  const [geocodingEstimate, setGeocodingEstimate] = useState<GeocodingEstimate | null>(null)
  const [rentalStartDate, setRentalStartDate] = useState(new Date().toISOString().slice(0, 10))
  const [rentalEndDate, setRentalEndDate] = useState(new Date().toISOString().slice(0, 10))
  const [requestModalOpen, setRequestModalOpen] = useState(false)
  const [requestModalBusy, setRequestModalBusy] = useState(false)

  useEffect(() => {
    const run = async () => {
      setLoading(true)
      setLoadFailed(false)
      try {
        const { data } = await api.get<ServiceItem>(`/services/${id}`)
        setItem({
          ...data,
          specs: data.specs ?? [],
          images: data.images ?? [],
          coverImageUrl: data.coverImageUrl ?? null,
          subtitle: data.subtitle ?? null,
        })
        setCarouselIndex(0)
        const rawDraft = localStorage.getItem(DRAFT_KEY)
        const drafts = rawDraft ? (JSON.parse(rawDraft) as Record<string, { notes?: string; estimate?: GeocodingEstimate }>) : {}
        const draft = drafts[String(data.id)]
        if (draft?.notes) setRequestNotes(draft.notes)
        if (draft?.estimate) {
          setGeocodingEstimate(draft.estimate)
          setSelectedPoint({ lat: draft.estimate.latitude, lng: draft.estimate.longitude })
        }
      } catch {
        notifyError('Не удалось загрузить карточку услуги.')
        setLoadFailed(true)
      } finally {
        setLoading(false)
      }
    }
    void run()
  }, [id, reloadKey])

  if (loading) return <section className="card"><p>Загрузка...</p></section>
  if (loadFailed && !item) {
    return (
      <section className="card">
        <p className="muted">Карточка услуги не загрузилась.</p>
        <button type="button" onClick={() => setReloadKey((k) => k + 1)}>
          Повторить
        </button>
      </section>
    )
  }
  if (!item) return <section className="card"><p className="muted">Услуга не найдена.</p></section>
  const currentItem = item
  const gallery = (currentItem.images ?? []).length > 0
    ? (currentItem.images ?? [])
    : [{ imageUrl: 'https://placehold.co/1200x700?text=Rental+Service', altText: currentItem.title, cover: true }]
  const activeImage = gallery[carouselIndex] ?? gallery[0]

  async function addSaved() {
    persistDraft()
    await api.post(`/user/saved-services/${currentItem.id}`)
    notifySuccess('Услуга добавлена в отложенные.')
  }

  async function createRequest() {
    setRequestModalBusy(true)
    try {
      await api.post('/user/service-requests', {
        serviceId: currentItem.id,
        rentalStartDate,
        rentalEndDate,
        notes: requestNotes,
        objectAddress: geocodingEstimate?.normalizedAddress ?? null,
      })
      notifySuccess('Заявка создана.')
      setRequestNotes('')
      setSelectedPoint(null)
      setGeocodingEstimate(null)
      setRequestModalOpen(false)
      const rawDraft = localStorage.getItem(DRAFT_KEY)
      const drafts = rawDraft ? (JSON.parse(rawDraft) as Record<string, unknown>) : {}
      delete drafts[String(currentItem.id)]
      localStorage.setItem(DRAFT_KEY, JSON.stringify(drafts))
    } catch (e) {
      if (axios.isAxiosError(e)) {
        const message = (e.response?.data as { message?: string } | undefined)?.message
        notifyError(message ?? 'Не удалось создать заявку.')
      } else {
        notifyError('Не удалось создать заявку.')
      }
    } finally {
      setRequestModalBusy(false)
    }
  }

  async function estimateByPoint(lat: number, lng: number) {
    try {
      const { data } = await api.get<GeocodingEstimate>('/user/service-requests/geocoding-estimate-by-point', {
        params: { lat, lng },
      })
      setGeocodingEstimate(data)
      setSelectedPoint({ lat, lng })
      persistDraft(data)
    } catch {
      notifyWarning('Не удалось рассчитать логистику для выбранной точки. Попробуйте выбрать точку рядом.')
    }
  }

  function persistDraft(estimate?: GeocodingEstimate) {
    const rawDraft = localStorage.getItem(DRAFT_KEY)
    const drafts = rawDraft ? (JSON.parse(rawDraft) as Record<string, { notes?: string; estimate?: GeocodingEstimate }>) : {}
    drafts[String(currentItem.id)] = { notes: requestNotes, estimate: estimate ?? geocodingEstimate ?? undefined }
    localStorage.setItem(DRAFT_KEY, JSON.stringify(drafts))
  }

  return (
    <section className="card details-layout">
      <div>
        <h2>{item.title}</h2>
        {currentItem.subtitle ? <p className="muted">{currentItem.subtitle}</p> : null}
      </div>
      <div className="details-grid">
        <div className="carousel-box">
          <img className="details-image" src={activeImage.imageUrl} alt={activeImage.altText ?? currentItem.title} />
          {gallery.length > 1 ? (
            <div className="carousel-controls">
              <button className="ghost" onClick={() => setCarouselIndex((prev) => (prev - 1 + gallery.length) % gallery.length)}>
                Назад
              </button>
              <span className="muted">{carouselIndex + 1} / {gallery.length}</span>
              <button className="ghost" onClick={() => setCarouselIndex((prev) => (prev + 1) % gallery.length)}>
                Вперед
              </button>
            </div>
          ) : null}
        </div>
        <div className="details-info">
          <div className="details-facts">
            {(currentItem.specs ?? []).map((spec) => (
              <p key={`${currentItem.id}-detail-${spec.key}`}>
                <strong>{spec.key}:</strong> {spec.value}
              </p>
            ))}
          </div>
          <p className="price-line">Стоимость: от <strong>{currentItem.basePrice} BYN/сутки</strong></p>
          <div className="row">
            <button onClick={addSaved}>В отложенные</button>
            <button onClick={() => { setRequestModalOpen(true) }}>Оформить заявку</button>
          </div>
        </div>
      </div>
      <p className="muted line-clamp">{currentItem.description}</p>
      {requestModalOpen ? (
        <div className="modal-overlay">
          <section className="modal-card">
            <h3>Оформление заявки</h3>
            <div className="form-grid">
              <label>
                Дата начала аренды
                <input type="date" value={rentalStartDate} onChange={(e) => setRentalStartDate(e.target.value)} />
              </label>
              <label>
                Дата окончания аренды
                <input type="date" value={rentalEndDate} onChange={(e) => setRentalEndDate(e.target.value)} />
              </label>
            </div>
            <div className="map-card">
              <ClickableMap selectedPoint={selectedPoint} onSelect={(lat, lng) => { void estimateByPoint(lat, lng) }} />
            </div>
            {geocodingEstimate ? (
              <p className="muted">
                {geocodingEstimate.normalizedAddress} · {geocodingEstimate.distanceKm} км · надбавка {geocodingEstimate.logisticsSurcharge} BYN
              </p>
            ) : (
              <p className="muted">Точка еще не выбрана.</p>
            )}
            <label>
              Комментарий к заявке
              <textarea
                value={requestNotes}
                onChange={(e) => setRequestNotes(e.target.value)}
                placeholder="Укажите объект, сроки и пожелания"
              />
            </label>
            {geocodingEstimate ? (
              <p className="price-line">
                Предварительно: <strong>{Number(currentItem.basePrice) + Number(geocodingEstimate.logisticsSurcharge)} BYN/сутки</strong>
              </p>
            ) : null}
            <div className="row">
              <button className="ghost" onClick={() => { setRequestModalOpen(false) }}>Отмена</button>
              <button onClick={() => { void createRequest() }} disabled={!geocodingEstimate || requestModalBusy}>
                {requestModalBusy ? 'Отправка...' : 'Подтвердить и отправить'}
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </section>
  )
}

function SavedPage() {
  const [saved, setSaved] = useState<SavedService[]>([])
  const [loading, setLoading] = useState(false)
  const [notesByService, setNotesByService] = useState<Record<number, string>>({})
  const [estimateByService, setEstimateByService] = useState<Record<number, GeocodingEstimate>>({})
  const [rentalStartDate, setRentalStartDate] = useState(new Date().toISOString().slice(0, 10))
  const [rentalEndDate, setRentalEndDate] = useState(new Date().toISOString().slice(0, 10))
  const [requestModalServiceId, setRequestModalServiceId] = useState<number | null>(null)
  const [modalPoint, setModalPoint] = useState<{ lat: number; lng: number } | null>(null)
  const [modalEstimate, setModalEstimate] = useState<GeocodingEstimate | null>(null)
  const [modalNotes, setModalNotes] = useState('')
  const [modalBusy, setModalBusy] = useState(false)

  async function load() {
    setLoading(true)
    const { data } = await api.get<SavedService[]>('/user/saved-services')
    setSaved(data)
    const rawDraft = localStorage.getItem(DRAFT_KEY)
    const drafts = rawDraft ? (JSON.parse(rawDraft) as Record<string, { estimate?: GeocodingEstimate; notes?: string }>) : {}
    const estimates: Record<number, GeocodingEstimate> = {}
    const notes: Record<number, string> = {}
    for (const item of data) {
      const draft = drafts[String(item.serviceId)]
      if (draft?.estimate) estimates[item.serviceId] = draft.estimate
      if (draft?.notes) notes[item.serviceId] = draft.notes
    }
    setEstimateByService(estimates)
    setNotesByService((prev) => ({ ...prev, ...notes }))
    setLoading(false)
  }

  useEffect(() => {
    void load()
  }, [])

  async function remove(id: number) {
    await api.delete(`/user/saved-services/${id}`)
    await load()
  }

  function openCreateRequestModal(id: number) {
    const estimate = estimateByService[id] ?? null
    setRequestModalServiceId(id)
    setModalEstimate(estimate)
    setModalPoint(estimate ? { lat: estimate.latitude, lng: estimate.longitude } : null)
    setModalNotes(notesByService[id] ?? '')
  }

  function closeCreateRequestModal() {
    setRequestModalServiceId(null)
    setModalPoint(null)
    setModalEstimate(null)
    setModalNotes('')
    setModalBusy(false)
  }

  async function estimateByPointForSaved(lat: number, lng: number) {
    setModalBusy(true)
    try {
      const { data } = await api.get<GeocodingEstimate>('/user/service-requests/geocoding-estimate-by-point', {
        params: { lat, lng },
      })
      setModalEstimate(data)
      setModalPoint({ lat, lng })
    } catch {
      notifyWarning('Не удалось рассчитать логистику для выбранной точки. Попробуйте другую точку на карте.')
    } finally {
      setModalBusy(false)
    }
  }

  async function createRequestFromModal() {
    if (!requestModalServiceId || !modalEstimate) {
      notifyInfo('Выберите точку на карте, чтобы рассчитать логистику.')
      return
    }
    setModalBusy(true)
    try {
      await api.post('/user/service-requests', {
        serviceId: requestModalServiceId,
        rentalStartDate,
        rentalEndDate,
        notes: modalNotes,
        objectAddress: modalEstimate.normalizedAddress,
      })
      notifySuccess('Заявка создана.')
      setNotesByService((prev) => ({ ...prev, [requestModalServiceId]: '' }))
      setEstimateByService((prev) => {
        const next = { ...prev }
        delete next[requestModalServiceId]
        return next
      })
      closeCreateRequestModal()
    } catch (e) {
      if (axios.isAxiosError(e)) {
        const message = (e.response?.data as { message?: string } | undefined)?.message
        notifyError(message ?? 'Не удалось создать заявку.')
      } else {
        notifyError('Не удалось создать заявку.')
      }
    } finally {
      setModalBusy(false)
    }
  }

  return (
    <section className="card page-section">
      <div className="page-section-head">
        <div>
          <h2>Отложенные услуги</h2>
          <p className="muted">Быстрый доступ к услугам, которые вы планируете арендовать.</p>
        </div>
      </div>
      {loading ? <p className="muted">Загрузка...</p> : null}
      <ul className="list">
        {saved.map((x) => (
          <li key={x.serviceId}>
            <div>
              <strong>{x.title}</strong>
              <p className="muted">{x.category} · {x.basePrice} BYN</p>
            </div>
            <div className="row list-actions">
              <button className="action-btn" onClick={() => openCreateRequestModal(x.serviceId)}>Оставить заявку</button>
              <button className="ghost action-btn" onClick={() => remove(x.serviceId)}>Удалить</button>
            </div>
          </li>
        ))}
      </ul>
      {requestModalServiceId ? (
        <div className="modal-overlay">
          <section className="modal-card">
            <h3>Оформление заявки</h3>
            <div className="form-grid">
              <label>
                Дата начала аренды
                <input type="date" value={rentalStartDate} onChange={(e) => setRentalStartDate(e.target.value)} />
              </label>
              <label>
                Дата окончания аренды
                <input type="date" value={rentalEndDate} onChange={(e) => setRentalEndDate(e.target.value)} />
              </label>
            </div>
            <div className="map-card">
              <ClickableMap
                selectedPoint={modalPoint}
                onSelect={(lat, lng) => { void estimateByPointForSaved(lat, lng) }}
              />
            </div>
            {modalEstimate ? (
              <p className="muted">
                {modalEstimate.normalizedAddress} · {modalEstimate.distanceKm} км · надбавка {modalEstimate.logisticsSurcharge} BYN
              </p>
            ) : (
              <p className="muted">Точка еще не выбрана.</p>
            )}
            <label>
              Комментарий к заявке
              <textarea
                placeholder="Укажите детали объекта и пожелания"
                value={modalNotes}
                onChange={(e) => setModalNotes(e.target.value)}
              />
            </label>
            <div className="row">
              <button className="ghost" onClick={closeCreateRequestModal}>Отмена</button>
              <button onClick={() => { void createRequestFromModal() }} disabled={!modalEstimate || modalBusy}>
                {modalBusy ? 'Расчет...' : 'Подтвердить и отправить'}
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </section>
  )
}

function RequestsPage() {
  const [items, setItems] = useState<ServiceRequest[]>([])
  const [editRequestId, setEditRequestId] = useState<number | null>(null)
  const [editRentalStartDate, setEditRentalStartDate] = useState(new Date().toISOString().slice(0, 10))
  const [editRentalEndDate, setEditRentalEndDate] = useState(new Date().toISOString().slice(0, 10))
  const [editPoint, setEditPoint] = useState<{ lat: number; lng: number } | null>(null)
  const [editEstimate, setEditEstimate] = useState<GeocodingEstimate | null>(null)
  const [editObjectAddress, setEditObjectAddress] = useState('')
  const [editBusy, setEditBusy] = useState(false)

  async function load() {
    const { data } = await api.get<ServiceRequest[]>('/user/service-requests')
    setItems(data)
  }

  useEffect(() => {
    void load()
  }, [])

  function extractAddressFromNotes(notes: string): string {
    const match = notes.match(/Адрес объекта:\s*([^|\n]+)/)
    return match ? match[1].trim() : ''
  }

  async function estimateByPointForEdit(lat: number, lng: number) {
    try {
      const { data } = await api.get<GeocodingEstimate>('/user/service-requests/geocoding-estimate-by-point', {
        params: { lat, lng },
      })
      setEditEstimate(data)
      setEditPoint({ lat, lng })
    } catch {
      notifyWarning('Не удалось рассчитать логистику для выбранной точки. Выберите другую точку.')
    }
  }

  async function saveRequestChanges(requestId: number) {
    const nextAddress = editEstimate?.normalizedAddress ?? editObjectAddress
    if (!nextAddress) {
      notifyInfo('Выберите точку на карте для перерасчета логистики.')
      return
    }
    setEditBusy(true)
    try {
      await api.patch(`/user/service-requests/${requestId}`, {
        rentalStartDate: editRentalStartDate,
        rentalEndDate: editRentalEndDate,
        objectAddress: nextAddress,
      })
      notifySuccess(`Заявка #${requestId} обновлена.`)
      setEditRequestId(null)
      setEditEstimate(null)
      setEditPoint(null)
      await load()
    } finally {
      setEditBusy(false)
    }
  }

  async function cancelRequest(requestId: number) {
    await api.delete(`/user/service-requests/${requestId}`)
    notifySuccess(`Заявка #${requestId} отозвана.`)
    await load()
  }

  async function removeHistoryRequest(requestId: number) {
    await api.delete(`/user/service-requests/${requestId}/history`)
    notifySuccess(`Заявка #${requestId} удалена из истории.`)
    await load()
  }

  async function clearHistory() {
    const { data } = await api.delete<{ deletedCount: number }>('/user/service-requests/history')
    notifySuccess(`Очищено заявок из истории: ${data.deletedCount}.`)
    await load()
  }

  async function markReturned(requestId: number) {
    await api.post(`/user/service-requests/${requestId}/return`)
    notifySuccess(`Возврат по заявке #${requestId} зафиксирован.`)
    await load()
  }

  return (
    <section className="card page-section">
      <div className="page-section-head">
        <div>
          <h2>Мои заявки</h2>
          <p className="muted">Контролируйте статус аренды, даты и возврат оборудования.</p>
        </div>
      </div>
      <div className="toolbar page-toolbar">
        <button className="ghost action-btn" onClick={clearHistory}>Очистить историю</button>
      </div>
      <ul className="list">
        {items.map((x) => (
          <li key={x.requestId}>
            <div>
              <strong>#{x.requestId} · {x.serviceTitle}</strong>
              <p><span className={`status-chip ${statusMeta(x.status).cls}`}>{statusMeta(x.status).label}</span></p>
              <p className="muted">
                Оборудование: #{x.equipmentId} · {x.equipmentInventoryCode} · {x.equipmentModelName}
              </p>
              <p className="muted">
                Период: {x.rentalStartDate} — {x.rentalEndDate} · Всего суток аренды: {x.rentalDays}
              </p>
              <p className="muted">Возврат: {x.returnedAt ? formatDate(x.returnedAt) : 'не зафиксирован'}</p>
              {x.notes ? <p className="muted">Комментарий: {x.notes}</p> : null}
            </div>
            <div className="row list-actions">
              {editRequestId === x.requestId ? (
                <>
                  <button className="action-btn" onClick={() => saveRequestChanges(x.requestId)}>Сохранить</button>
                  <button className="ghost action-btn" onClick={() => setEditRequestId(null)}>Отмена</button>
                </>
              ) : (
                <>
                  {(x.status === 'NEW' || x.status === 'AWAITING_PAYMENT') ? (
                    <button
                      className="action-btn"
                      onClick={() => {
                        setEditRequestId(x.requestId)
                        setEditRentalStartDate(x.rentalStartDate)
                        setEditRentalEndDate(x.rentalEndDate)
                        setEditEstimate(null)
                        setEditPoint(null)
                        setEditObjectAddress(extractAddressFromNotes(x.notes))
                      }}
                    >
                      Редактировать
                    </button>
                  ) : null}
                  {(x.status === 'NEW' || x.status === 'AWAITING_PAYMENT') ? (
                    <button className="ghost action-btn" onClick={() => cancelRequest(x.requestId)}>Отозвать</button>
                  ) : null}
                  {x.status === 'IN_PROGRESS' && !x.returnedAt ? (
                    <button className="action-btn" onClick={() => markReturned(x.requestId)}>Отметить возврат</button>
                  ) : null}
                  {(x.status === 'COMPLETED' || x.status === 'CANCELLED') ? (
                    <button className="ghost action-btn" onClick={() => removeHistoryRequest(x.requestId)}>Удалить из истории</button>
                  ) : null}
                </>
              )}
            </div>
          </li>
        ))}
      </ul>
      {editRequestId ? (
        <div className="modal-overlay">
          <section className="modal-card">
            <h3>Редактирование заявки #{editRequestId}</h3>
            <div className="form-grid">
              <label>
                Дата начала аренды
                <input type="date" value={editRentalStartDate} onChange={(e) => setEditRentalStartDate(e.target.value)} />
              </label>
              <label>
                Дата окончания аренды
                <input type="date" value={editRentalEndDate} onChange={(e) => setEditRentalEndDate(e.target.value)} />
              </label>
            </div>
            <div className="map-card">
              <ClickableMap
                selectedPoint={editPoint}
                onSelect={(lat, lng) => { void estimateByPointForEdit(lat, lng) }}
              />
            </div>
            {editEstimate ? (
              <p className="muted">
                {editEstimate.normalizedAddress} · {editEstimate.distanceKm} км · надбавка {editEstimate.logisticsSurcharge} BYN
              </p>
            ) : (
              <p className="muted">Точка еще не выбрана.</p>
            )}
            <div className="row">
              <button className="ghost" onClick={() => setEditRequestId(null)}>Отмена</button>
              <button onClick={() => { void saveRequestChanges(editRequestId) }} disabled={!(editEstimate || editObjectAddress) || editBusy}>
                Сохранить
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </section>
  )
}

function InvoicesPage() {
  const [items, setItems] = useState<Invoice[]>([])

  async function load() {
    const { data } = await api.get<Invoice[]>('/user/invoices')
    setItems(data)
  }

  useEffect(() => {
    void load()
  }, [])

  async function payInvoice(id: number) {
    try {
      await api.post(`/user/invoices/${id}/pay`)
      await load()
      notifySuccess('Счёт оплачен виртуальными средствами.')
    } catch (e) {
      if (axios.isAxiosError(e)) {
        const message = (e.response?.data as { message?: string } | undefined)?.message
        notifyError(message ?? 'Не удалось оплатить счет.')
      } else {
        notifyError('Не удалось оплатить счет.')
      }
    }
  }

  return (
    <section className="card page-section">
      <div className="page-section-head">
        <div>
          <h2>Мои счета</h2>
          <p className="muted">Оплачивайте счета с виртуального баланса и отслеживайте историю платежей.</p>
        </div>
      </div>
      <ul className="list">
        {items.map((x) => {
          const st = invoiceDisplayStatus(x.status)
          const subtitled = userInvoiceSubtitleLines(x)
          const currencyShown = invoiceCurrencyLabel(x.currency)
          return (
            <li key={x.id}>
              <div>
                <p className="row" style={{ flexWrap: 'wrap', alignItems: 'center', gap: '0.5rem' }}>
                  <strong>{userInvoicePrimaryTitle(x)}</strong>
                  <span className={`status-chip ${st.cls}`}>{st.label}</span>
                </p>
                {subtitled.map((line, idx) => (
                  <p key={idx} className="muted">{line}</p>
                ))}
                <p className="muted">
                  {Number(x.amount).toFixed(2)} {currencyShown}
                  {' · '}
                  выставлен {formatDate(x.createdAt)}
                  {x.paidAt ? ` · оплачен ${formatDate(x.paidAt)}` : null}
                </p>
              </div>
              {x.status === 'ISSUED' ? (
                <div className="row">
                  <button onClick={() => payInvoice(x.id)}>Оплатить</button>
                </div>
              ) : null}
            </li>
          )
        })}
      </ul>
    </section>
  )
}

function ProfilePage() {
  const [profile, setProfile] = useState<Profile | null>(null)
  const [profileEditModalOpen, setProfileEditModalOpen] = useState(false)
  const [editEmail, setEditEmail] = useState('')
  const [editFullName, setEditFullName] = useState('')
  const [editPhoneNationalDigits, setEditPhoneNationalDigits] = useState('')
  const [passwordModalOpen, setPasswordModalOpen] = useState(false)
  const [oldPassword, setOldPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [newPasswordRepeat, setNewPasswordRepeat] = useState('')
  const [topUpModalOpen, setTopUpModalOpen] = useState(false)
  const [topUpAmount, setTopUpAmount] = useState('100')
  const [topUpCode, setTopUpCode] = useState('')
  const [topUpCodeSent, setTopUpCodeSent] = useState(false)
  const [topUpCodeExpiresHint, setTopUpCodeExpiresHint] = useState<string | null>(null)
  const setEmail = useAuthStore((s) => s.setEmail)
  const setBalance = useAuthStore((s) => s.setBalance)

  function openTopUpModal() {
    setTopUpModalOpen(true)
    setTopUpAmount('100')
    setTopUpCode('')
    setTopUpCodeSent(false)
    setTopUpCodeExpiresHint(null)
  }

  function closeTopUpModal() {
    setTopUpModalOpen(false)
    setTopUpCodeExpiresHint(null)
  }

  function openProfileEditModal() {
    if (!profile) return
    setEditEmail(profile.email)
    setEditFullName(profile.fullName)
    setEditPhoneNationalDigits(belarusNationalDigitsFromInput(profile.phoneNumber ?? ''))
    setProfileEditModalOpen(true)
  }

  function closeProfileEditModal() {
    setProfileEditModalOpen(false)
  }

  function openPasswordModal() {
    setOldPassword('')
    setNewPassword('')
    setNewPasswordRepeat('')
    setPasswordModalOpen(true)
  }

  function closePasswordModal() {
    setPasswordModalOpen(false)
    setOldPassword('')
    setNewPassword('')
    setNewPasswordRepeat('')
  }

  useEffect(() => {
    const run = async () => {
      try {
        const { data } = await api.get<Profile>('/auth/me')
        setProfile(data)
        setBalance(data.virtualBalance)
      } catch {
        notifyError('Не удалось загрузить профиль.')
      }
    }
    void run()
  }, [])

  async function saveProfile(e: FormEvent) {
    e.preventDefault()
    try {
      const digits = editPhoneNationalDigits.replace(/\D/g, '')
      let phonePayload: string
      if (digits.length === 0) {
        phonePayload = ''
      } else {
        const canon = canonicalBelarusPhoneNational(editPhoneNationalDigits)
        if (!canon) {
          notifyWarning('Телефон: укажите полностью номер после +375 — код оператора 17/25/29/33/44 и 7 цифр.')
          return
        }
        phonePayload = canon
      }

      const { data } = await api.patch<Profile>('/auth/me', {
        email: editEmail,
        fullName: editFullName,
        phoneNumber: phonePayload,
      })
      setProfile(data)
      setEmail(data.email)
      setBalance(data.virtualBalance)
      notifySuccess('Профиль обновлен.')
      closeProfileEditModal()
    } catch (e) {
      if (axios.isAxiosError(e)) {
        const message = (e.response?.data as { message?: string } | undefined)?.message
        notifyError(message ?? 'Не удалось сохранить профиль.')
      } else {
        notifyError('Не удалось сохранить профиль.')
      }
    }
  }

  async function changePassword(e: FormEvent) {
    e.preventDefault()
    if (newPassword !== newPasswordRepeat) {
      notifyWarning('Новый пароль и подтверждение не совпадают.')
      return
    }
    try {
      await api.patch('/auth/me/password', { oldPassword, newPassword })
      notifySuccess('Пароль успешно изменен. Выполните повторный вход на других устройствах.')
      closePasswordModal()
    } catch {
      notifyError('Не удалось изменить пароль. Проверьте старый пароль и требования к новому.')
    }
  }

  async function requestTopUpCode() {
    try {
      const { data } = await api.post<{ message: string; expiresAt: string }>('/auth/me/top-up/request-code', {
        amount: Number(topUpAmount),
      })
      setTopUpCodeSent(true)
      setTopUpCodeExpiresHint(formatDate(data.expiresAt))
    } catch {
      notifyError('Не удалось отправить код пополнения.')
    }
  }

  async function confirmTopUp() {
    try {
      const { data } = await api.post<{ virtualBalance: number; message: string }>('/auth/me/top-up/confirm', {
        amount: Number(topUpAmount),
        code: topUpCode,
      })
      setProfile((prev) => prev ? { ...prev, virtualBalance: data.virtualBalance } : prev)
      setBalance(data.virtualBalance)
      notifySuccess(`Баланс пополнен. Текущий баланс: ${data.virtualBalance} BYN.`)
      closeTopUpModal()
    } catch {
      notifyError('Не удалось подтвердить пополнение.')
    }
  }

  return (
    <section className="card profile-card">
      <div className="profile-header">
        <div>
          <h2>Личный кабинет</h2>
          <p className="muted">Управляйте данными, безопасностью и финансами аккаунта.</p>
        </div>
      </div>
      {profile ? (
        <>
          <div className="profile-overview">
            <article className="profile-stat profile-stat--balance">
              <span className="profile-stat__label">Текущий баланс</span>
              <strong>{profile.virtualBalance.toFixed(2)} BYN</strong>
            </article>
            <article className="profile-stat">
              <span className="profile-stat__label">Email</span>
              <strong>{profile.email}</strong>
            </article>
            <article className="profile-stat">
              <span className="profile-stat__label">ФИО</span>
              <strong>{profile.fullName}</strong>
            </article>
            <article className="profile-stat">
              <span className="profile-stat__label">Телефон</span>
              <strong>{profile.phoneNumber ?? '-'}</strong>
            </article>
          </div>
          <div className="toolbar profile-actions">
            <button type="button" onClick={openTopUpModal}><span className="btn-with-icon"><IconPlus className="ui-icon" />Пополнить баланс</span></button>
            <button type="button" className="ghost" onClick={openProfileEditModal}><span className="btn-with-icon"><IconEdit className="ui-icon" />Изменить данные</span></button>
            <button type="button" className="ghost" onClick={openPasswordModal}><span className="btn-with-icon"><IconLock className="ui-icon" />Изменить пароль</span></button>
          </div>
          {profileEditModalOpen ? (
            <div className="modal-overlay">
              <section className="modal-card">
                <h3>Изменение данных</h3>
                <form className="form-grid" onSubmit={saveProfile}>
                  <label>
                    Email
                    <input value={editEmail} onChange={(e) => setEditEmail(e.target.value)} required />
                  </label>
                  <label>
                    ФИО
                    <input value={editFullName} onChange={(e) => setEditFullName(e.target.value)} required />
                  </label>
                  <label>
                    Телефон
                    <input
                      value={formatBelarusPhoneMasked(editPhoneNationalDigits)}
                      inputMode="numeric"
                      autoComplete="tel-national"
                      placeholder="+375 (__) ___-__-__"
                      onChange={(e) => setEditPhoneNationalDigits(belarusNationalDigitsFromInput(e.target.value))}
                    />
                    <span className="muted">Набирайте только цифры: +375 подставится автоматически.</span>
                  </label>
                  <div className="row">
                    <button type="button" className="ghost" onClick={closeProfileEditModal}>Отмена</button>
                    <button type="submit">Сохранить</button>
                  </div>
                </form>
              </section>
            </div>
          ) : null}
          {passwordModalOpen ? (
            <div className="modal-overlay">
              <section className="modal-card">
                <h3>Смена пароля</h3>
                <form className="form-grid" onSubmit={changePassword}>
                  <label>
                    Текущий пароль
                    <input
                      type="password"
                      value={oldPassword}
                      onChange={(e) => setOldPassword(e.target.value)}
                      autoComplete="current-password"
                      required
                    />
                  </label>
                  <label>
                    Новый пароль
                    <input
                      type="password"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      autoComplete="new-password"
                      required
                    />
                  </label>
                  <label>
                    Повторите новый пароль
                    <input
                      type="password"
                      value={newPasswordRepeat}
                      onChange={(e) => setNewPasswordRepeat(e.target.value)}
                      autoComplete="new-password"
                      required
                    />
                  </label>
                  <div className="row">
                    <button type="button" className="ghost" onClick={closePasswordModal}>Отмена</button>
                    <button type="submit">Сохранить пароль</button>
                  </div>
                </form>
              </section>
            </div>
          ) : null}
          {topUpModalOpen ? (
            <div className="modal-overlay">
              <section className="modal-card">
                <h3>Пополнение баланса</h3>
                <div className="form-grid">
                  <label>
                    Сумма (BYN)
                    <input
                      type="number"
                      min={1}
                      step="1"
                      value={topUpAmount}
                      onChange={(e) => setTopUpAmount(e.target.value)}
                      required
                    />
                  </label>
                  <button type="button" onClick={() => { void requestTopUpCode() }}>Отправить код на email</button>
                  {topUpCodeSent && topUpCodeExpiresHint ? (
                    <p className="muted">Код отправлен. Действителен до: {topUpCodeExpiresHint}</p>
                  ) : null}
                  <label>
                    Код из письма
                    <input
                      value={topUpCode}
                      onChange={(e) => setTopUpCode(e.target.value)}
                      placeholder="Цифры кода из email"
                      inputMode="numeric"
                    />
                  </label>
                  <div className="row">
                    <button type="button" className="ghost" onClick={closeTopUpModal}>Отмена</button>
                    <button type="button" disabled={!topUpCodeSent} onClick={() => { void confirmTopUp() }}>
                      Подтвердить пополнение
                    </button>
                  </div>
                </div>
              </section>
            </div>
          ) : null}
        </>
      ) : (
        <p className="muted">Загрузка...</p>
      )}
    </section>
  )
}

function ManagerPage() {
  const [managerView, setManagerView] = useState<
    'users' | 'requests' | 'services' | 'defects' | 'billing' | 'reports'
  >('users')
  const [users, setUsers] = useState<AdminUser[]>([])
  const [userQuery, setUserQuery] = useState('')
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null)
  const [userModalOpen, setUserModalOpen] = useState(false)
  const [isCreatingUser, setIsCreatingUser] = useState(false)
  const [userForm, setUserForm] = useState({
    email: '',
    fullName: '',
    password: '',
    status: 'ACTIVE' as UserStatus,
    role: 'USER' as Role,
  })
  const [requests, setRequests] = useState<ManagerServiceRequest[]>([])
  const [requestStatusFilter, setRequestStatusFilter] = useState<string>('')
  const [requestSummary, setRequestSummary] = useState<Record<string, number>>({})

  const [managerServices, setManagerServices] = useState<ServiceItem[]>([])
  const [serviceEditId, setServiceEditId] = useState<number | null>(null)
  const [serviceModalOpen, setServiceModalOpen] = useState(false)
  const [serviceForm, setServiceForm] = useState({
    title: '',
    subtitle: '',
    description: '',
    category: '',
    basePrice: '',
    active: true,
    specsText: '',
    imagesText: '',
  })
  const [defects, setDefects] = useState<DefectReport[]>([])
  const [defectSummary, setDefectSummary] = useState<Record<string, number>>({})
  const [defectStatusFilter, setDefectStatusFilter] = useState<string>('')

  const [invoiceCandidates, setInvoiceCandidates] = useState<ManagerServiceRequest[]>([])
  /** Заявка, с которой перешли из списка (пока кандидаты на счёт ещё не подгрузились). */
  const [billingPresetFromRequest, setBillingPresetFromRequest] = useState<ManagerServiceRequest | null>(null)
  const [selectedInvoiceRequestId, setSelectedInvoiceRequestId] = useState<number | null>(null)
  const [invoiceBaseAmount, setInvoiceBaseAmount] = useState('')
  const [invoiceLogisticsAmount, setInvoiceLogisticsAmount] = useState('')
  const [invoiceDescription, setInvoiceDescription] = useState('')

  const [reportFromDate, setReportFromDate] = useState('')
  const [reportToDate, setReportToDate] = useState('')
  const [report, setReport] = useState<RevenueReport | null>(null)
  const [analytics, setAnalytics] = useState<AnalyticsReport | null>(null)
  const [invoiceSubmitting, setInvoiceSubmitting] = useState(false)

  const invoiceFormOptions = useMemo(() => {
    if (
      billingPresetFromRequest &&
      !invoiceCandidates.some((c) => c.requestId === billingPresetFromRequest.requestId)
    ) {
      return [billingPresetFromRequest, ...invoiceCandidates]
    }
    return invoiceCandidates
  }, [billingPresetFromRequest, invoiceCandidates])

  async function loadUsers() {
    const { data } = await api.get<Paged<AdminUser>>('/manager/users', { params: { query: userQuery, size: 20 } })
    setUsers(data.content ?? [])
  }

  async function loadRequests() {
    const params = requestStatusFilter ? { status: requestStatusFilter, size: 20 } : { size: 20 }
    const { data } = await api.get<Paged<ManagerServiceRequest>>('/manager/service-requests', { params })
    setRequests(data.content ?? [])
    const summaryResponse = await api.get<Record<string, number>>('/manager/service-requests/summary')
    setRequestSummary(summaryResponse.data)
  }

  async function loadManagerServices() {
    const { data } = await api.get<Paged<ServiceItem>>('/services', { params: { active: true, size: 40, sort: 'id,desc' } })
    setManagerServices(data.content ?? [])
  }

  async function loadDefects() {
    const params = defectStatusFilter ? { status: defectStatusFilter } : undefined
    const { data } = await api.get<DefectReport[]>('/manager/defect-reports', { params })
    setDefects(data)
    const summaryResponse = await api.get<Record<string, number>>('/manager/defect-reports/summary')
    setDefectSummary(summaryResponse.data)
  }

  async function loadInvoiceCandidates() {
    const { data } = await api.get<ManagerServiceRequest[]>('/manager/service-requests/invoice-candidates')
    setInvoiceCandidates(data)
    if (!selectedInvoiceRequestId && data.length > 0) {
      const first = data[0]
      setSelectedInvoiceRequestId(first.requestId)
      setInvoiceBaseAmount(String(Number(first.serviceBasePrice) * Number(first.rentalDays || 1)))
      setInvoiceLogisticsAmount(extractLogisticsSurcharge(first.notes))
      setInvoiceDescription(`Счет за услугу "${first.serviceTitle}" по заявке #${first.requestId}`)
    }
  }

  async function bootstrap() {
    try {
      await Promise.all([loadUsers(), loadRequests(), loadManagerServices(), loadDefects(), loadInvoiceCandidates()])
    } catch {
      notifyError('Не удалось загрузить данные менеджерского кабинета.')
    }
  }

  useEffect(() => {
    void bootstrap()
  }, [])

  async function selectUser(userId: number) {
    const { data } = await api.get<AdminUser>(`/manager/users/${userId}`)
    setSelectedUserId(data.id)
    setUserForm({
      email: data.email,
      fullName: data.fullName,
      password: '',
      status: data.status,
      role: data.role,
    })
    setIsCreatingUser(false)
    setUserModalOpen(true)
  }

  async function updateSelectedUser() {
    if (!selectedUserId && !isCreatingUser) return
    if (isCreatingUser) {
      await api.post('/manager/users', userForm)
      notifySuccess('Пользователь создан.')
    } else {
      await api.patch(`/manager/users/${selectedUserId}`, {
        email: userForm.email,
        fullName: userForm.fullName,
        password: userForm.password || null,
        status: userForm.status,
        role: userForm.role,
      })
      notifySuccess('Профиль пользователя обновлен.')
    }
    setUserModalOpen(false)
    await loadUsers()
  }

  async function deleteSelectedUser() {
    if (!selectedUserId) return
    await api.delete(`/manager/users/${selectedUserId}`)
    notifySuccess('Пользователь удален.')
    setUserModalOpen(false)
    await loadUsers()
  }

  async function updateRequestStatus(requestId: number, status: ServiceRequestStatus) {
    try {
      await api.patch(`/manager/service-requests/${requestId}/status`, { status })
      notifySuccess(`Статус заявки #${requestId} изменен на ${status}.`)
      await loadRequests()
    } catch (e) {
      if (axios.isAxiosError(e)) {
        const message = (e.response?.data as { message?: string } | undefined)?.message
        notifyError(message ?? 'Не удалось изменить статус заявки.')
      } else {
        notifyError('Не удалось изменить статус заявки.')
      }
    }
  }

  async function submitServiceForm(e: FormEvent) {
    e.preventDefault()
    const payload = {
      title: serviceForm.title,
      subtitle: serviceForm.subtitle || null,
      description: serviceForm.description,
      category: serviceForm.category,
      basePrice: Number(serviceForm.basePrice),
      active: serviceForm.active,
      specs: serviceForm.specsText
        .split('\n')
        .map((x) => x.trim())
        .filter(Boolean)
        .map((line) => {
          const [key, ...rest] = line.split(':')
          return { key: key.trim(), value: rest.join(':').trim() }
        })
        .filter((x) => x.key && x.value),
      images: serviceForm.imagesText
        .split('\n')
        .map((x) => x.trim())
        .filter(Boolean)
        .map((url, idx) => ({ imageUrl: url, altText: `${serviceForm.title} ${idx + 1}`, cover: idx === 0 })),
    }
    if (serviceEditId) {
      await api.put(`/manager/services/${serviceEditId}`, payload)
      notifySuccess(`Услуга #${serviceEditId} обновлена.`)
    } else {
      await api.post('/manager/services', payload)
      notifySuccess('Новая услуга добавлена в каталог.')
    }
    setServiceEditId(null)
    setServiceForm({ title: '', subtitle: '', description: '', category: '', basePrice: '', active: true, specsText: '', imagesText: '' })
    setServiceModalOpen(false)
    await loadManagerServices()
  }

  async function activateService(serviceId: number) {
    await api.patch(`/manager/services/${serviceId}/activate`)
    await loadManagerServices()
  }

  async function deactivateService(serviceId: number) {
    await api.delete(`/manager/services/${serviceId}`)
    await loadManagerServices()
  }

  async function updateDefectStatus(defectId: number, status: DefectReportStatus) {
    await api.patch(`/manager/defect-reports/${defectId}/status`, { status })
    notifySuccess(`Дефектная ведомость #${defectId}: ${defectStatusMeta(status).label}.`)
    await loadDefects()
  }

  async function createInvoice(e: FormEvent) {
    e.preventDefault()
    if (invoiceSubmitting) return
    const selected = invoiceFormOptions.find((x) => x.requestId === selectedInvoiceRequestId)
    if (!selected) {
      notifyWarning('Выберите заявку для выставления счета.')
      return
    }
    setInvoiceSubmitting(true)
    try {
      const total = Number(invoiceBaseAmount || 0) + Number(invoiceLogisticsAmount || 0)
      await api.post('/manager/invoices', {
        userId: selected.userId,
        serviceRequestId: selected.requestId,
        defectReportId: null,
        invoiceType: 'SERVICE',
        amount: total,
        description: invoiceDescription,
      })
      notifySuccess(`Счет по заявке #${selected.requestId} отправлен клиенту ${selected.userEmail}.`)
      setSelectedInvoiceRequestId(null)
      setBillingPresetFromRequest(null)
      setInvoiceBaseAmount('')
      setInvoiceLogisticsAmount('')
      setInvoiceDescription('')
      await loadInvoiceCandidates()
      await loadRequests()
    } catch (e) {
      if (axios.isAxiosError(e)) {
        const message = (e.response?.data as { message?: string } | undefined)?.message
        notifyError(message ?? 'Не удалось выставить счет.')
      } else {
        notifyError('Не удалось выставить счет.')
      }
    } finally {
      setInvoiceSubmitting(false)
    }
  }

  function extractLogisticsSurcharge(notes?: string) {
    if (!notes) return '0'
    const match = notes.match(/Логистическая надбавка:\s*([0-9]+(?:[.,][0-9]+)?)/i)
    if (!match) return '0'
    return match[1].replace(',', '.')
  }

  function goToInvoiceForRequest(req: ManagerServiceRequest) {
    setBillingPresetFromRequest(req)
    setManagerView('billing')
    setSelectedInvoiceRequestId(req.requestId)
    setInvoiceBaseAmount(String(Number(req.serviceBasePrice) * Number(req.rentalDays || 1)))
    setInvoiceLogisticsAmount(extractLogisticsSurcharge(req.notes))
    setInvoiceDescription(`Счет за услугу "${req.serviceTitle}" по заявке #${req.requestId}`)
    void loadInvoiceCandidates()
  }

  async function deleteCancelledRequestByManager(requestId: number) {
    try {
      await api.delete(`/manager/service-requests/${requestId}`)
      notifySuccess(`Отмененная заявка #${requestId} удалена.`)
      await loadRequests()
      await loadInvoiceCandidates()
    } catch (e) {
      if (axios.isAxiosError(e)) {
        const message = (e.response?.data as { message?: string } | undefined)?.message
        notifyError(message ?? 'Не удалось удалить заявку.')
      } else {
        notifyError('Не удалось удалить заявку.')
      }
    }
  }

  async function buildRevenueReport(e: FormEvent) {
    e.preventDefault()
    const from = parseIsoDate(reportFromDate)
    const to = parseIsoDate(reportToDate)
    if (!from || !to) {
      notifyWarning('Проверьте даты отчета.')
      return
    }
    if (to < from) {
      notifyWarning('Конечная дата не может быть раньше начальной.')
      return
    }
    const { data } = await api.get<RevenueReport>('/manager/reports/revenue', {
      params: { fromDate: reportFromDate, toDate: reportToDate },
    })
    setReport(data)
    const analyticsResponse = await api.get<AnalyticsReport>('/manager/reports/analytics', {
      params: { fromDate: reportFromDate, toDate: reportToDate },
    })
    setAnalytics(analyticsResponse.data)
  }

  function exportReportToExcel() {
    if (!report) {
      notifyInfo('Сначала постройте отчет.')
      return
    }
    const workbook = XLSX.utils.book_new()
    const summaryRows = [
      { KPI: 'Период', Значение: `${report.fromDate} — ${report.toDate}` },
      { KPI: 'Оплачено счетов', Значение: report.paidInvoices },
      { KPI: 'Общая выручка', Значение: report.totalRevenue },
      { KPI: 'Сервисная выручка', Значение: report.serviceRevenue },
      { KPI: 'Штрафная выручка', Значение: report.penaltyRevenue },
      { KPI: 'Создано заявок', Значение: analytics?.requestsCreated ?? 0 },
      { KPI: 'Выставлено счетов', Значение: analytics?.invoicesIssued ?? 0 },
      { KPI: 'Конверсия оплат, %', Значение: analytics?.paymentConversionPercent ?? 0 },
      { KPI: 'Средний оплаченный чек', Значение: analytics?.averagePaidCheck ?? 0 },
    ]
    XLSX.utils.book_append_sheet(workbook, XLSX.utils.json_to_sheet(summaryRows), 'Сводка')
    if (analytics) {
      XLSX.utils.book_append_sheet(workbook, XLSX.utils.json_to_sheet(analytics.daily), 'Динамика')
      const statusRows = Object.entries(analytics.requestStatusBreakdown).map(([status, count]) => ({
        Статус: status,
        Количество: count,
      }))
      XLSX.utils.book_append_sheet(workbook, XLSX.utils.json_to_sheet(statusRows), 'Статусы')
      XLSX.utils.book_append_sheet(
        workbook,
        XLSX.utils.json_to_sheet(analytics.topServices.map((row) => ({
          serviceId: row.serviceId,
          serviceTitle: row.serviceTitle,
          requestsCount: row.requestsCount,
          revenue: row.revenue,
        }))),
        'Топ услуг'
      )
    }
    XLSX.writeFile(workbook, `revenue-report-${report.fromDate}_${report.toDate}.xlsx`)
  }

  return (
    <section className="card page-section manager-shell">
      <h2>Панель менеджера</h2>
      <div className="toolbar manager-tabs">
        <button className={managerView === 'users' ? 'active-tab manager-tab-btn' : 'ghost manager-tab-btn'} onClick={() => setManagerView('users')}>Пользователи</button>
        <button className={managerView === 'requests' ? 'active-tab manager-tab-btn' : 'ghost manager-tab-btn'} onClick={() => setManagerView('requests')}>Заявки</button>
        <button className={managerView === 'services' ? 'active-tab manager-tab-btn' : 'ghost manager-tab-btn'} onClick={() => setManagerView('services')}>Каталог услуг</button>
        <button className={managerView === 'defects' ? 'active-tab manager-tab-btn' : 'ghost manager-tab-btn'} onClick={() => setManagerView('defects')}>Дефектные ведомости</button>
        <button className={managerView === 'billing' ? 'active-tab manager-tab-btn' : 'ghost manager-tab-btn'} onClick={() => setManagerView('billing')}>Финансы</button>
        <button className={managerView === 'reports' ? 'active-tab manager-tab-btn' : 'ghost manager-tab-btn'} onClick={() => setManagerView('reports')}>Отчеты</button>
      </div>

      {managerView === 'users' ? <div className="card section-card manager-panel">
        <h3>Пользователи и роли</h3>
        <div className="toolbar">
          <input placeholder="Поиск email/ФИО" value={userQuery} onChange={(e) => setUserQuery(e.target.value)} />
          <button onClick={loadUsers}>Найти</button>
          <button
            onClick={() => {
              setIsCreatingUser(true)
              setSelectedUserId(null)
              setUserForm({ email: '', fullName: '', password: '', status: 'ACTIVE', role: 'USER' })
              setUserModalOpen(true)
            }}
          >
            Создать пользователя
          </button>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>ФИО</th>
                <th>Email</th>
                <th>Статус</th>
                <th>Роль</th>
                <th>Действия</th>
              </tr>
            </thead>
            <tbody>
              {users.map((x) => (
                <tr key={x.id}>
                  <td>#{x.id}</td>
                  <td>{x.fullName}</td>
                  <td>{x.email}</td>
                  <td>
                    <span className={`status-chip ${x.status === 'ACTIVE' ? 'user-status-active' : 'user-status-blocked'}`}>
                      {x.status === 'ACTIVE' ? 'Активный' : 'Заблокированный'}
                    </span>
                  </td>
                  <td>
                    <span className={`status-chip ${
                      x.role === 'MANAGER' ? 'role-manager' : x.role === 'SERVICE_SPECIALIST' ? 'role-specialist' : 'role-user'
                    }`}>
                      {x.role}
                    </span>
                  </td>
                  <td><button onClick={() => selectUser(x.id)}>Редактировать</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {userModalOpen ? (
          <div className="modal-overlay">
            <div className="modal-card">
              <h4>{isCreatingUser ? 'Создание пользователя' : `Редактирование #${selectedUserId}`}</h4>
              <div className="form-grid">
                <label>
                  Email
                  <input value={userForm.email} onChange={(e) => setUserForm((p) => ({ ...p, email: e.target.value }))} />
                </label>
                <label>
                  ФИО
                  <input value={userForm.fullName} onChange={(e) => setUserForm((p) => ({ ...p, fullName: e.target.value }))} />
                </label>
                <label>
                  {isCreatingUser ? 'Пароль' : 'Новый пароль (опционально)'}
                  <input type="password" value={userForm.password} onChange={(e) => setUserForm((p) => ({ ...p, password: e.target.value }))} />
                </label>
                <label>
                  Статус
                  <select value={userForm.status} onChange={(e) => setUserForm((p) => ({ ...p, status: e.target.value as UserStatus }))}>
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="BLOCKED">BLOCKED</option>
                  </select>
                </label>
                <label>
                  Роль
                  <select value={userForm.role} onChange={(e) => setUserForm((p) => ({ ...p, role: e.target.value as Role }))}>
                    <option value="USER">USER</option>
                    <option value="MANAGER">MANAGER</option>
                    <option value="SERVICE_SPECIALIST">SERVICE_SPECIALIST</option>
                  </select>
                </label>
              </div>
              <div className="toolbar">
                <button className="action-btn" onClick={updateSelectedUser}>Сохранить</button>
                {!isCreatingUser ? <button className="ghost action-btn" onClick={deleteSelectedUser}>Удалить</button> : null}
                <button className="ghost action-btn" onClick={() => setUserModalOpen(false)}>Закрыть</button>
              </div>
            </div>
          </div>
        ) : null}
      </div> : null}

      {managerView === 'requests' ? <div className="card section-card manager-panel">
        <h3>Заявки и контроль workflow</h3>
        <div className="toolbar page-toolbar">
          <select value={requestStatusFilter} onChange={(e) => setRequestStatusFilter(e.target.value)}>
            <option value="">Все статусы</option>
            <option value="NEW">Новая</option>
            <option value="AWAITING_PAYMENT">Ожидает оплаты</option>
            <option value="IN_PROGRESS">В работе</option>
            <option value="AWAITING_SPECIALIST_REVIEW">Ожидает заключения специалиста</option>
            <option value="COMPLETED">Завершена</option>
            <option value="CANCELLED">Отменена</option>
          </select>
          <button onClick={loadRequests}>Обновить</button>
        </div>
        <div className="summary-chips">
          <span className="status-chip status-new">Новые: {requestSummary.new ?? 0}</span>
          <span className="status-chip status-payment">Ожидают оплаты: {requestSummary.awaitingPayment ?? 0}</span>
          <span className="status-chip status-progress">В работе: {requestSummary.inProgress ?? 0}</span>
          <span className="status-chip status-feedback">Ожидают заключения специалиста: {requestSummary.awaitingSpecialistReview ?? 0}</span>
          <span className="status-chip status-completed">Завершены: {requestSummary.completed ?? 0}</span>
          <span className="status-chip status-cancelled">Отменены: {requestSummary.cancelled ?? 0}</span>
        </div>
        <ul className="list">
          {requests.map((x) => (
            <li key={x.requestId}>
              <div>
                <strong>#{x.requestId} · {x.serviceTitle}</strong>
                <p><span className={`status-chip ${statusMeta(x.status).cls}`}>{statusMeta(x.status).label}</span> · <span className="muted">{formatDate(x.createdAt)}</span></p>
                {x.notes ? <p className="muted">{x.notes}</p> : null}
              </div>
              <div className="row list-actions">
                {(x.status === 'NEW') ? (
                  <button type="button" className="action-btn" onClick={() => goToInvoiceForRequest(x)}>
                    Выставить счёт
                  </button>
                ) : null}
                {(x.status === 'AWAITING_PAYMENT') ? (
                  <button className="action-btn" onClick={() => updateRequestStatus(x.requestId, 'IN_PROGRESS')}>В работу</button>
                ) : null}
                {(x.status === 'NEW' || x.status === 'AWAITING_PAYMENT') ? (
                  <button className="ghost action-btn" onClick={() => updateRequestStatus(x.requestId, 'CANCELLED')}>Отменить</button>
                ) : null}
                {x.status === 'CANCELLED' ? (
                  <button className="ghost action-btn" onClick={() => deleteCancelledRequestByManager(x.requestId)}>Удалить</button>
                ) : null}
              </div>
            </li>
          ))}
        </ul>
      </div> : null}

      {managerView === 'services' ? <div className="card section-card manager-panel">
        <h3>Услуги каталога</h3>
        <div className="toolbar">
          <button
            onClick={() => {
              setServiceEditId(null)
              setServiceForm({ title: '', subtitle: '', description: '', category: '', basePrice: '', active: true, specsText: '', imagesText: '' })
              setServiceModalOpen(true)
            }}
          >
            Создать услугу
          </button>
        </div>
        {serviceModalOpen ? <div className="modal-overlay"><form className="form-grid modal-card" onSubmit={submitServiceForm}>
          <label>
            Название услуги
            <input
              value={serviceForm.title}
              onChange={(e) => setServiceForm((prev) => ({ ...prev, title: e.target.value }))}
              required
            />
          </label>
          <label>
            Подзаголовок
            <input value={serviceForm.subtitle} onChange={(e) => setServiceForm((prev) => ({ ...prev, subtitle: e.target.value }))} />
          </label>
          <label>
            Категория
            <input
              value={serviceForm.category}
              onChange={(e) => setServiceForm((prev) => ({ ...prev, category: e.target.value }))}
              required
            />
          </label>
          <label>
            Описание
            <textarea
              value={serviceForm.description}
              onChange={(e) => setServiceForm((prev) => ({ ...prev, description: e.target.value }))}
              required
            />
          </label>
          <label>
            Базовая стоимость
            <input
              type="number"
              min={0}
              step="0.01"
              value={serviceForm.basePrice}
              onChange={(e) => setServiceForm((prev) => ({ ...prev, basePrice: e.target.value }))}
              required
            />
          </label>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={serviceForm.active}
              onChange={(e) => setServiceForm((prev) => ({ ...prev, active: e.target.checked }))}
            />
            Активна
          </label>
          <label>
            Характеристики (каждая строка: Ключ: Значение)
            <textarea value={serviceForm.specsText} onChange={(e) => setServiceForm((prev) => ({ ...prev, specsText: e.target.value }))} />
          </label>
          <label>
            Фото URL (по одному на строку, первая строка — обложка)
            <textarea value={serviceForm.imagesText} onChange={(e) => setServiceForm((prev) => ({ ...prev, imagesText: e.target.value }))} />
          </label>
          <button type="submit">{serviceEditId ? 'Обновить услугу' : 'Создать услугу'}</button>
          <button type="button" className="ghost" onClick={() => setServiceModalOpen(false)}>Закрыть</button>
        </form></div> : null}
        {Array.from(
          managerServices.reduce((acc, item) => {
            const key = item.category || 'Прочее'
            const list = acc.get(key) ?? []
            list.push(item)
            acc.set(key, list)
            return acc
          }, new Map<string, ServiceItem[]>()).entries()
        ).map(([category, items]) => (
          <section key={category} className="category-section">
            <h4>{category}</h4>
            <div className="table-wrap">
              <table className="data-table manager-services-table">
                <colgroup>
                  <col className="manager-col-id" />
                  <col className="manager-col-service" />
                  <col className="manager-col-price" />
                  <col className="manager-col-status" />
                  <col className="manager-col-actions" />
                </colgroup>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Услуга</th>
                    <th>Цена</th>
                    <th>Статус</th>
                    <th>Действия</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((x) => (
                    <tr key={x.id}>
                      <td>#{x.id}</td>
                      <td>{x.title}</td>
                      <td>{x.basePrice} BYN</td>
                      <td>
                        <span className={`status-chip ${x.active ? 'user-status-active' : 'user-status-blocked'}`}>
                          {x.active ? 'Активна' : 'Отключена'}
                        </span>
                      </td>
                      <td>
                        <div className="row manager-table-actions">
                          <button
                            className="action-btn action-btn--sm"
                            onClick={() => {
                              setServiceEditId(x.id)
                              setServiceForm({
                                title: x.title,
                                subtitle: x.subtitle ?? '',
                                description: x.description,
                                category: x.category,
                                basePrice: String(x.basePrice),
                                active: x.active,
                                specsText: (x.specs ?? []).map((s) => `${s.key}: ${s.value}`).join('\n'),
                                imagesText: (x.images ?? []).map((i) => i.imageUrl).join('\n'),
                              })
                              setServiceModalOpen(true)
                            }}
                          >
                            Изменить
                          </button>
                          {x.active ? (
                            <button className="ghost action-btn action-btn--sm" onClick={() => deactivateService(x.id)}>
                              Деактивировать
                            </button>
                          ) : (
                            <button className="action-btn action-btn--sm" onClick={() => activateService(x.id)}>
                              Активировать
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        ))}
      </div> : null}

      {managerView === 'defects' ? <div className="card section-card manager-panel">
        <h3>Дефектные ведомости</h3>
        <div className="toolbar page-toolbar">
          <select value={defectStatusFilter} onChange={(e) => setDefectStatusFilter(e.target.value)}>
            <option value="">Все статусы</option>
            <option value="NEW">Новая</option>
            <option value="SENT_TO_MANAGER">Передана менеджеру</option>
            <option value="APPROVED">Утверждена</option>
            <option value="REJECTED">Отклонена</option>
          </select>
          <button onClick={loadDefects}>Обновить</button>
        </div>
        <div className="toolbar" style={{ flexWrap: 'wrap' }}>
          {Object.entries(defectSummary).map(([k, v]) => (
            <span key={k} className={`status-chip ${
              k === 'new' ? 'status-new'
                : k === 'sentToManager' ? 'status-feedback'
                  : k === 'approved' ? 'status-completed'
                    : k === 'rejected' ? 'status-cancelled'
                      : 'status-new'
            }`}>
              {MANAGER_DEFECT_SUMMARY_LABELS[k] ?? k}: {v}
            </span>
          ))}
        </div>
        <ul className="list">
          {defects.map((x) => (
            <li key={x.id}>
              <div>
                <p className="row" style={{ flexWrap: 'wrap', alignItems: 'center', gap: '0.5rem' }}>
                  <strong>
                    #{x.id} · {x.equipmentInventoryCode} · {x.equipmentModelName}
                  </strong>
                  <span className={`status-chip ${defectStatusMeta(x.status).cls}`}>{defectStatusMeta(x.status).label}</span>
                  <span className={`status-chip ${defectSeverityChipClass(x.severity)}`}>{defectSeverityRu(x.severity)}</span>
                </p>
                <p className="muted">
                  Инв. № {x.equipmentInventoryCode}
                  {x.serviceRequestId != null ? ` · Заявка №${x.serviceRequestId}` : null}
                  {' · '}
                  {formatDate(x.createdAt)}
                </p>
                <p>
                  <span className={`status-chip ${managerDefectPenaltyPaymentMeta(x).cls}`}>
                    {managerDefectPenaltyPaymentMeta(x).label}
                  </span>
                </p>
                <p className="muted">{x.defectDescription}</p>
              </div>
              {x.status === 'SENT_TO_MANAGER' ? (
                <div className="row list-actions">
                  <button className="action-btn" onClick={() => updateDefectStatus(x.id, 'APPROVED')}>Утвердить</button>
                  <button className="ghost action-btn" onClick={() => updateDefectStatus(x.id, 'REJECTED')}>Отклонить</button>
                </div>
              ) : null}
            </li>
          ))}
        </ul>
      </div> : null}

      {managerView === 'billing' ? <div className="card section-card manager-panel">
        <h3>Финансы и счета</h3>
        <form className="form-grid" onSubmit={createInvoice}>
          <select
            value={selectedInvoiceRequestId ?? ''}
            onChange={(e) => {
              setBillingPresetFromRequest(null)
              const selected = invoiceFormOptions.find((x) => x.requestId === Number(e.target.value))
              if (!selected) {
                setSelectedInvoiceRequestId(null)
                return
              }
              setSelectedInvoiceRequestId(selected.requestId)
              setInvoiceBaseAmount(String(Number(selected.serviceBasePrice) * Number(selected.rentalDays || 1)))
              setInvoiceLogisticsAmount(extractLogisticsSurcharge(selected.notes))
              setInvoiceDescription(`Счет за услугу "${selected.serviceTitle}" по заявке #${selected.requestId}`)
            }}
            required
          >
            <option value="">Выберите заявку</option>
            {invoiceFormOptions.map((x) => (
              <option key={x.requestId} value={x.requestId}>
                #{x.requestId} · {x.serviceTitle} · {x.userEmail}
              </option>
            ))}
          </select>
          {selectedInvoiceRequestId ? (() => {
            const selected = invoiceFormOptions.find((x) => x.requestId === selectedInvoiceRequestId)
            if (!selected) return null
            return (
              <div className="invoice-receipt">
                <div className="receipt-head">
                  <strong>Сервисный чек</strong>
                  <span className="muted">Заявка #{selected.requestId}</span>
                </div>
                <p><strong>Клиент:</strong> {selected.userEmail}</p>
                <p><strong>Услуга:</strong> {selected.serviceTitle}</p>
                <p><strong>Период аренды:</strong> {selected.rentalStartDate} — {selected.rentalEndDate} ({selected.rentalDays} суток)</p>
                <p><strong>Тариф:</strong> {Number(selected.serviceBasePrice).toFixed(2)} BYN/сутки</p>
                <p><strong>Комментарий:</strong> {selected.notes || '-'}</p>
                <hr />
                <p><span>Аренда ({selected.rentalDays} суток)</span><strong>{Number(invoiceBaseAmount || 0).toFixed(2)} BYN</strong></p>
                <p><span>Логистическая надбавка</span><strong>{Number(invoiceLogisticsAmount || 0).toFixed(2)} BYN</strong></p>
                <p className="receipt-total"><span>Итого к оплате</span><strong>{(Number(invoiceBaseAmount || 0) + Number(invoiceLogisticsAmount || 0)).toFixed(2)} BYN</strong></p>
              </div>
            )
          })() : null}
          <label>
            Базовая сумма
            <input
              type="number"
              min={0}
              step="0.01"
              value={invoiceBaseAmount}
              onChange={(e) => setInvoiceBaseAmount(e.target.value)}
              required
            />
          </label>
          <label>
            Логистическая надбавка
            <input
              type="number"
              min={0}
              step="0.01"
              value={invoiceLogisticsAmount}
              onChange={(e) => setInvoiceLogisticsAmount(e.target.value)}
              required
            />
          </label>
          <label>
            Описание счета
            <textarea
              value={invoiceDescription}
              onChange={(e) => setInvoiceDescription(e.target.value)}
              required
            />
          </label>
          <button className="action-btn" type="submit" disabled={!selectedInvoiceRequestId || invoiceSubmitting}>
            {invoiceSubmitting ? 'Отправка...' : 'Отправить чек клиенту'}
          </button>
        </form>
      </div> : null}

      {managerView === 'reports' ? <div className="card section-card manager-panel">
        <h3>Отчет доходности</h3>
        <form className="toolbar page-toolbar" onSubmit={buildRevenueReport}>
          <input type="date" value={reportFromDate} onChange={(e) => setReportFromDate(e.target.value)} required />
          <input type="date" value={reportToDate} onChange={(e) => setReportToDate(e.target.value)} required />
          <button className="action-btn" type="submit">Построить отчет</button>
          <button type="button" className="ghost action-btn" onClick={exportReportToExcel} disabled={!report}>
            Выгрузить в Excel
          </button>
        </form>
        {report ? (
          <div className="form-grid">
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>KPI</th>
                    <th>Значение</th>
                  </tr>
                </thead>
                <tbody>
                  <tr><td>Период</td><td>{report.fromDate} — {report.toDate}</td></tr>
                  <tr><td>Оплачено счетов</td><td>{report.paidInvoices}</td></tr>
                  <tr><td>Общая выручка</td><td>{report.totalRevenue}</td></tr>
                  <tr><td>Сервисная выручка</td><td>{report.serviceRevenue}</td></tr>
                  <tr><td>Штрафная выручка</td><td>{report.penaltyRevenue}</td></tr>
                  <tr><td>Создано заявок</td><td>{analytics?.requestsCreated ?? 0}</td></tr>
                  <tr><td>Выставлено счетов</td><td>{analytics?.invoicesIssued ?? 0}</td></tr>
                  <tr><td>Конверсия оплат, %</td><td>{analytics?.paymentConversionPercent ?? 0}</td></tr>
                  <tr><td>Средний оплаченный чек</td><td>{analytics?.averagePaidCheck ?? 0}</td></tr>
                </tbody>
              </table>
            </div>

            {analytics ? (
              <>
                <div className="chart-box">
                  <h4>Динамика выручки по дням</h4>
                  <ResponsiveContainer width="100%" height={260}>
                    <LineChart data={analytics.daily}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="date" />
                      <YAxis />
                      <Tooltip />
                      <Legend />
                      <Line type="monotone" dataKey="revenue" name="Выручка" stroke="#2563eb" strokeWidth={2} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>

                <div className="chart-box">
                  <h4>Динамика заявок и оплат</h4>
                  <ResponsiveContainer width="100%" height={260}>
                    <BarChart data={analytics.daily}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="date" />
                      <YAxis />
                      <Tooltip />
                      <Legend />
                      <Bar dataKey="requestsCreated" name="Заявки" fill="#f59e0b" />
                      <Bar dataKey="paidInvoices" name="Оплаченные счета" fill="#16a34a" />
                    </BarChart>
                  </ResponsiveContainer>
                </div>

                <div className="table-wrap">
                  <h4>Распределение заявок по статусам</h4>
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Статус</th>
                        <th>Количество</th>
                      </tr>
                    </thead>
                    <tbody>
                      {Object.entries(analytics.requestStatusBreakdown).map(([k, v]) => (
                        <tr key={k}>
                          <td>
                            <span className={`status-chip ${formatServiceRequestStatus(k).cls}`}>
                              {formatServiceRequestStatus(k).label}
                            </span>
                          </td>
                          <td>{v}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                <div className="table-wrap">
                  <h4>Топ услуг по выручке</h4>
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Услуга</th>
                        <th>Заявок</th>
                        <th>Выручка</th>
                      </tr>
                    </thead>
                    <tbody>
                      {analytics.topServices.map((row) => (
                        <tr key={row.serviceId}>
                          <td>{row.serviceTitle}</td>
                          <td>{row.requestsCount}</td>
                          <td>{row.revenue}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            ) : null}
          </div>
        ) : (
          <EmptyState text="Укажите период, чтобы получить сводку." />
        )}
      </div> : null}
    </section>
  )
}

function SpecialistPage() {
  const [specialistView, setSpecialistView] = useState<'equipment' | 'defects'>('equipment')
  const [equipmentOverview, setEquipmentOverview] = useState<SpecialistEquipmentOverview[]>([])
  const [equipmentInRepair, setEquipmentInRepair] = useState<SpecialistEquipmentOverview[]>([])
  const [equipmentDetailModal, setEquipmentDetailModal] = useState<{
    row: SpecialistEquipmentOverview
    timeline: EquipmentStateTimelineEntry[]
  } | null>(null)
  const [equipmentDetailLoading, setEquipmentDetailLoading] = useState(false)
  const [inspectEquipmentRow, setInspectEquipmentRow] = useState<SpecialistEquipmentOverview | null>(null)
  const [activeRequests, setActiveRequests] = useState<SpecialistServiceRequestOption[]>([])
  const [equipmentId, setEquipmentId] = useState('')
  const [inspectConditionScore, setInspectConditionScore] = useState(70)
  const [batteryPercent, setBatteryPercent] = useState('')
  const [temperatureCelsius, setTemperatureCelsius] = useState('')
  const [operatingHoursDelta, setOperatingHoursDelta] = useState('')
  const [notes, setNotes] = useState('')
  const [repairAlerts, setRepairAlerts] = useState<EquipmentRepairAlert[]>([])

  const [defectServiceRequestId, setDefectServiceRequestId] = useState('')
  const [defectDescription, setDefectDescription] = useState('')
  const [defectSeverity, setDefectSeverity] = useState<DefectSeverity>('MEDIUM')
  const [recommendedPenalty, setRecommendedPenalty] = useState('')
  const [defectStatusFilter, setDefectStatusFilter] = useState('')
  const [defects, setDefects] = useState<DefectReport[]>([])
  const [defectSummary, setDefectSummary] = useState<Record<string, number>>({})
  const [defectEditModal, setDefectEditModal] = useState<DefectReport | null>(null)
  const [editDefectDescription, setEditDefectDescription] = useState('')
  const [editDefectSeverity, setEditDefectSeverity] = useState<DefectSeverity>('MEDIUM')
  const [editDefectPenalty, setEditDefectPenalty] = useState('')

  function equipmentHealth(wearPercent: number) {
    if (wearPercent <= 20) return { label: 'Отличное', cls: 'status-completed' }
    if (wearPercent <= 45) return { label: 'Рабочее', cls: 'status-progress' }
    if (wearPercent <= 70) return { label: 'Требует внимания', cls: 'status-feedback' }
    return { label: 'Критичный износ', cls: 'status-cancelled' }
  }

  function equipmentLifecycleMeta(status: string) {
    const map: Record<string, { label: string; cls: string }> = {
      AVAILABLE: { label: 'Доступно', cls: 'status-completed' },
      NEEDS_REPAIR: { label: 'Нужен ремонт', cls: 'status-feedback' },
      UNDER_REPAIR: { label: 'В ремонте', cls: 'status-payment' },
    }
    return map[status] ?? { label: status, cls: 'status-new' }
  }

  function timelineEventMeta(eventType: string) {
    if (eventType === 'INSPECTION') return { chip: 'Осмотр', cls: 'status-progress' }
    if (eventType === 'RENTAL_WEAR') return { chip: 'Износ аренды', cls: 'status-completed' }
    if (eventType === 'DEFECT_WEAR') return { chip: 'Дефект при приёме', cls: 'status-cancelled' }
    return { chip: eventType, cls: 'status-new' }
  }

  function timelineEntryChip(ev: EquipmentStateTimelineEntry) {
    if (ev.eventType === 'INSPECTION' && ev.title === 'Возврат из ремонта') {
      return { chip: 'Ремонт', cls: 'status-completed' }
    }
    return timelineEventMeta(ev.eventType)
  }

  function conditionLabelByScore(score: number): string {
    if (score >= 85) return 'ОТЛИЧНОЕ'
    if (score >= 65) return 'ХОРОШЕЕ'
    if (score >= 45) return 'УДОВЛЕТВОРИТЕЛЬНОЕ'
    if (score >= 25) return 'ПЛОХОЕ'
    return 'АВАРИЙНОЕ'
  }

  function conditionLabelRuByScore(score: number): string {
    if (score >= 85) return 'Отличное'
    if (score >= 65) return 'Хорошее'
    if (score >= 45) return 'Удовлетворительное'
    if (score >= 25) return 'Плохое'
    return 'Аварийное'
  }

  function scoreByConditionLabel(label?: string | null): number {
    if (label === 'ОТЛИЧНОЕ') return 92
    if (label === 'ХОРОШЕЕ') return 74
    if (label === 'УДОВЛЕТВОРИТЕЛЬНОЕ') return 54
    if (label === 'ПЛОХОЕ') return 32
    if (label === 'АВАРИЙНОЕ') return 12
    return 70
  }

  const selectedDefectRequest = activeRequests.find((x) => String(x.requestId) === defectServiceRequestId) ?? null

  async function loadEquipmentOverviewAndAlerts() {
    const inRepairPromise = api
      .get<SpecialistEquipmentOverview[]>('/specialist/equipment/in-repair')
      .catch((e) => {
        if (axios.isAxiosError(e) && e.response?.status === 404) {
          return { data: [] as SpecialistEquipmentOverview[] }
        }
        return Promise.reject(e)
      })
    const [overviewResponse, alertsResponse, inRepairResponse] = await Promise.all([
      api.get<SpecialistEquipmentOverview[]>('/specialist/equipment/state-overview'),
      api.get<EquipmentRepairAlert[]>('/specialist/equipment/repair-alerts'),
      inRepairPromise,
    ])
    setEquipmentOverview(overviewResponse.data)
    setRepairAlerts(alertsResponse.data)
    setEquipmentInRepair(inRepairResponse.data)
  }

  function openInspectModal(row: SpecialistEquipmentOverview) {
    setInspectEquipmentRow(row)
    setEquipmentId(String(row.equipmentId))
    setInspectConditionScore(scoreByConditionLabel(row.latestInspection?.conditionLabel))
    setBatteryPercent('')
    setTemperatureCelsius('')
    setOperatingHoursDelta('')
    setNotes('')
  }

  async function loadSpecialistCatalogs() {
    const [requestsResponse] = await Promise.all([
      api.get<SpecialistServiceRequestOption[]>('/specialist/service-requests/active'),
    ])
    setActiveRequests(requestsResponse.data)
    if (!defectServiceRequestId && requestsResponse.data.length > 0) {
      setDefectServiceRequestId(String(requestsResponse.data[0].requestId))
    }
    await loadEquipmentOverviewAndAlerts()
  }

  async function openEquipmentDetail(row: SpecialistEquipmentOverview) {
    setEquipmentDetailModal({ row, timeline: [] })
    setEquipmentDetailLoading(true)
    try {
      const { data } = await api.get<EquipmentStateTimelineEntry[]>(`/specialist/equipment/${row.equipmentId}/state-timeline`)
      setEquipmentDetailModal({ row, timeline: data })
    } catch {
      notifyError('Не удалось загрузить подробную историю по оборудованию.')
      setEquipmentDetailModal(null)
    } finally {
      setEquipmentDetailLoading(false)
    }
  }

  async function loadDefects() {
    try {
      const params = defectStatusFilter ? { status: defectStatusFilter } : undefined
      const { data } = await api.get<DefectReport[]>('/specialist/defect-reports', { params })
      setDefects(data)
      const summaryResponse = await api.get<Record<string, number>>('/specialist/defect-reports/summary')
      setDefectSummary(summaryResponse.data)
    } catch (e) {
      if (axios.isAxiosError(e) && e.response?.status === 404) {
        setDefectSummary({ new: 0, sentToManager: 0, approved: 0, rejected: 0 })
        return
      }
      throw e
    }
  }

  useEffect(() => {
    void loadSpecialistCatalogs()
  }, [])

  useEffect(() => {
    void loadDefects().catch(() => {
      notifyError('Не удалось загрузить дефектные ведомости.')
    })
  }, [defectStatusFilter])

  useEffect(() => {
    if (specialistView !== 'equipment') return
    void loadEquipmentOverviewAndAlerts()
    const intervalId = window.setInterval(() => {
      void loadEquipmentOverviewAndAlerts()
    }, 20000)
    return () => window.clearInterval(intervalId)
  }, [specialistView])

  async function recordState(e: FormEvent) {
    e.preventDefault()
    const conditionLabel = conditionLabelByScore(inspectConditionScore)
    try {
      const { data } = await api.post<EquipmentState>(`/specialist/equipment/${Number(equipmentId)}/state`, {
        conditionLabel,
        batteryPercent: batteryPercent ? Number(batteryPercent) : null,
        temperatureCelsius: temperatureCelsius ? Number(temperatureCelsius) : null,
        operatingHoursDelta: operatingHoursDelta ? Number(operatingHoursDelta) : null,
        notes,
      })
      if (data.criticalConditionDetected) {
        notifyWarning(
          'Состояние зафиксировано. Обнаружено критическое состояние, оборудование добавлено в уведомления на ремонт.',
        )
      } else {
        notifySuccess('Состояние оборудования зафиксировано.')
      }
      await loadEquipmentOverviewAndAlerts()
      setInspectEquipmentRow(null)
      setInspectConditionScore(70)
      setBatteryPercent('')
      setTemperatureCelsius('')
      setOperatingHoursDelta('')
      setNotes('')
    } catch {
      notifyError('Не удалось записать состояние оборудования.')
    }
  }

  function openDefectEditModal(report: DefectReport) {
    setDefectEditModal(report)
    setEditDefectDescription(report.defectDescription)
    setEditDefectSeverity(report.severity)
    setEditDefectPenalty(String(report.recommendedPenalty))
  }

  async function submitDefectEdit(e: FormEvent) {
    e.preventDefault()
    if (!defectEditModal) return
    const noDefects = editDefectDescription.trim().startsWith('Дефекты не выявлены')
    try {
      await api.patch(`/specialist/defect-reports/${defectEditModal.id}`, {
        defectDescription: editDefectDescription.trim(),
        severity: noDefects ? 'LOW' : editDefectSeverity,
        recommendedPenalty: noDefects ? 0 : Number(editDefectPenalty),
      })
      notifySuccess(`Ведомость №${defectEditModal.id} обновлена.`)
      setDefectEditModal(null)
      await loadDefects()
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const message = (err.response?.data as { message?: string } | undefined)?.message
        notifyError(message ?? 'Не удалось сохранить изменения.')
      } else {
        notifyError('Не удалось сохранить изменения.')
      }
    }
  }

  async function createDefect(e: FormEvent) {
    e.preventDefault()
    if (!defectServiceRequestId) {
      notifyWarning('Выберите заявку для оформления ведомости.')
      return
    }
    try {
      await api.post('/specialist/defect-reports', {
        serviceRequestId: Number(defectServiceRequestId),
        defectDescription,
        severity: defectSeverity,
        recommendedPenalty: Number(recommendedPenalty),
      })
      notifySuccess('Дефектная ведомость создана. Передайте ее менеджеру после проверки.')
      setDefectDescription('')
      setRecommendedPenalty('')
      await loadDefects()
    } catch {
      notifyError('Не удалось создать дефектную ведомость.')
    }
  }

  async function createNoDefectsConclusion() {
    if (!defectServiceRequestId) {
      notifyWarning('Выберите заявку.')
      return
    }
    try {
      await api.post('/specialist/defect-reports/no-defects', {
        serviceRequestId: Number(defectServiceRequestId),
        conclusionText: 'Проверка выполнена, критичных дефектов не обнаружено.',
      })
      notifySuccess(`Заключение об отсутствии дефектов по заявке #${defectServiceRequestId} отправлено менеджеру.`)
      await loadDefects()
    } catch (e) {
      if (axios.isAxiosError(e)) {
        const message = (e.response?.data as { message?: string } | undefined)?.message
        notifyError(message ?? 'Не удалось отправить заключение.')
      } else {
        notifyError('Не удалось отправить заключение.')
      }
    }
  }

  async function sendToManager(defectId: number) {
    try {
      await api.patch(`/specialist/defect-reports/${defectId}/status`, { status: 'SENT_TO_MANAGER' })
      notifySuccess(`Ведомость #${defectId} передана менеджеру.`)
      await loadDefects()
    } catch (e) {
      if (axios.isAxiosError(e)) {
        const message = (e.response?.data as { message?: string } | undefined)?.message
        notifyError(message ?? 'Не удалось передать ведомость менеджеру.')
      } else {
        notifyError('Не удалось передать ведомость менеджеру.')
      }
    }
  }

  async function sendEquipmentToRepair(equipmentIdToRepair: number) {
    try {
      await api.post(`/specialist/equipment/${equipmentIdToRepair}/send-to-repair`)
      notifySuccess(`Оборудование #${equipmentIdToRepair} отправлено в ремонт.`)
      await loadSpecialistCatalogs()
    } catch (e) {
      if (axios.isAxiosError(e)) {
        const message = (e.response?.data as { message?: string } | undefined)?.message
        notifyError(message ?? 'Не удалось отправить оборудование в ремонт.')
      } else {
        notifyError('Не удалось отправить оборудование в ремонт.')
      }
    }
  }

  async function completeEquipmentRepair(equipmentIdToRepair: number) {
    try {
      await api.post(`/specialist/equipment/${equipmentIdToRepair}/complete-repair`)
      notifySuccess(`Ремонт оборудования #${equipmentIdToRepair} завершен.`)
      await loadSpecialistCatalogs()
    } catch (e) {
      if (axios.isAxiosError(e)) {
        const message = (e.response?.data as { message?: string } | undefined)?.message
        notifyError(message ?? 'Не удалось завершить ремонт оборудования.')
      } else {
        notifyError('Не удалось завершить ремонт оборудования.')
      }
    }
  }

  return (
    <>
    <section className="card page-section specialist-shell">
      <h2>Панель сервисного специалиста</h2>

      <div className="toolbar specialist-tabs">
        <button className={specialistView === 'equipment' ? 'active-tab specialist-tab-btn' : 'ghost specialist-tab-btn'} onClick={() => setSpecialistView('equipment')}>
          Состояние оборудования
        </button>
        <button className={specialistView === 'defects' ? 'active-tab specialist-tab-btn' : 'ghost specialist-tab-btn'} onClick={() => setSpecialistView('defects')}>
          Дефектные ведомости
        </button>
      </div>

      {specialistView === 'equipment' ? (
        <div className="card section-card specialist-panel">
          <h3>Текущее состояние оборудования</h3>
          {equipmentOverview.length === 0 ? (
            <p className="muted">Нет единиц для отображения (всё у клиентов, в ремонте или справочник пуст).</p>
          ) : (
            Array.from(
              equipmentOverview.reduce((acc, item) => {
                const key = item.category || 'Прочее'
                const list = acc.get(key) ?? []
                list.push(item)
                acc.set(key, list)
                return acc
              }, new Map<string, SpecialistEquipmentOverview[]>()).entries(),
            ).map(([category, rows]) => (
              <section key={category} className="category-section">
                <h4>{category}</h4>
                <div className="table-wrap">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>Инв. №</th>
                        <th>Модель</th>
                        <th>Статус</th>
                        <th>Накопл. износ</th>
                        <th>% / сутки</th>
                        <th>Последний осмотр</th>
                        <th />
                      </tr>
                    </thead>
                    <tbody>
                      {rows.map((x) => (
                        <tr key={x.equipmentId}>
                          <td>#{x.equipmentId}</td>
                          <td>{x.inventoryCode}</td>
                          <td>{x.modelName}</td>
                          <td>
                            <span className={`status-chip ${equipmentLifecycleMeta(x.lifecycleStatus).cls}`}>
                              {equipmentLifecycleMeta(x.lifecycleStatus).label}
                            </span>
                          </td>
                          <td>{Number(x.accumulatedWearPercent).toFixed(2)}%</td>
                          <td>{Number(x.rentalWearRatePerDay).toFixed(4)}</td>
                          <td className="muted">
                            {x.latestInspection
                              ? `${x.latestInspection.conditionLabel} · ${Number(x.latestInspection.calculatedWearPercent).toFixed(1)}% · ${formatDate(x.latestInspection.recordedAt)}`
                              : '—'}
                          </td>
                          <td>
                            <div className="row specialist-table-actions">
                              <button type="button" className="action-btn action-btn--sm" onClick={() => openInspectModal(x)}>
                                Осмотр
                              </button>
                              <button type="button" className="ghost action-btn action-btn--sm" onClick={() => void openEquipmentDetail(x)}>
                                Подробнее
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            ))
          )}
          <div className="card section-card specialist-subpanel">
            <h4>Уведомления о критическом состоянии</h4>
            {repairAlerts.length === 0 ? (
              <p className="muted">Критических единиц оборудования нет.</p>
            ) : (
              <ul className="list">
                {repairAlerts.map((x) => (
                  <li key={x.equipmentId}>
                    <div>
                      <strong>#{x.equipmentId} · {x.inventoryCode} · {x.modelName}</strong>
                      <p className="muted">
                        Накопл. износ (эксплуатация): {Number(x.accumulatedWearPercent).toFixed(2)}% · последний осмотр:{' '}
                        {x.latestConditionLabel ?? '—'} · износ по осмотру {x.latestWearPercent ?? '—'}% ·{' '}
                        {x.latestRecordedAt ? formatDate(x.latestRecordedAt) : 'дата не указана'}
                        {x.latestWearPercent != null ? (
                          <>
                            {' '}
                            <span className={`status-chip ${equipmentHealth(Number(x.latestWearPercent)).cls}`}>
                              {equipmentHealth(Number(x.latestWearPercent)).label}
                            </span>
                          </>
                        ) : null}
                      </p>
                    </div>
                    <div className="row list-actions">
                      <button className="action-btn" onClick={() => sendEquipmentToRepair(x.equipmentId)}>Отправить в ремонт</button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
            <h4 className="specialist-subsection-title">Оборудование в ремонте</h4>
            {equipmentInRepair.length === 0 ? (
              <p className="muted">Сейчас в ремонте ничего нет.</p>
            ) : (
              Array.from(
                equipmentInRepair.reduce((acc, item) => {
                  const key = item.category || 'Прочее'
                  const list = acc.get(key) ?? []
                  list.push(item)
                  acc.set(key, list)
                  return acc
                }, new Map<string, SpecialistEquipmentOverview[]>()).entries(),
              ).map(([category, rows]) => (
                <section key={`repair-${category}`} className="category-section">
                  <h5>{category}</h5>
                  <div className="table-wrap">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>ID</th>
                          <th>Инв. №</th>
                          <th>Модель</th>
                          <th>Накопл. износ</th>
                          <th />
                        </tr>
                      </thead>
                      <tbody>
                        {rows.map((x) => (
                          <tr key={`ir-${x.equipmentId}`}>
                            <td>#{x.equipmentId}</td>
                            <td>{x.inventoryCode}</td>
                            <td>{x.modelName}</td>
                            <td>{Number(x.accumulatedWearPercent).toFixed(2)}%</td>
                            <td>
                              <div className="row specialist-table-actions">
                                <button type="button" className="action-btn action-btn--sm" onClick={() => void completeEquipmentRepair(x.equipmentId)}>
                                  Вернуть из ремонта
                                </button>
                                <button type="button" className="ghost action-btn action-btn--sm" onClick={() => void openEquipmentDetail(x)}>
                                  Подробнее
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </section>
              ))
            )}
          </div>
        </div>
      ) : null}

      {specialistView === 'defects' ? (
        <div className="card section-card specialist-panel">
          <h3>Дефектные ведомости</h3>
          <form className="form-grid" onSubmit={createDefect}>
            <select
              value={defectServiceRequestId}
              onChange={(e) => setDefectServiceRequestId(e.target.value)}
              required
            >
              <option value="">Выберите заявку</option>
              {activeRequests.map((x) => (
                <option key={x.requestId} value={x.requestId}>
                  #{x.requestId} · {x.serviceTitle} · {x.userEmail}
                </option>
              ))}
            </select>
            <div className="muted">
              Оборудование по заявке:{' '}
              {selectedDefectRequest
                ? `#${selectedDefectRequest.equipmentId} · ${selectedDefectRequest.equipmentInventoryCode} · ${selectedDefectRequest.equipmentModelName}`
                : '—'}
            </div>
            <div className="muted">
              Последнее состояние:{' '}
              {selectedDefectRequest?.latestEquipmentState
                ? `${selectedDefectRequest.latestEquipmentState.conditionLabel}, износ ${selectedDefectRequest.latestEquipmentState.calculatedWearPercent}% (${formatDate(selectedDefectRequest.latestEquipmentState.recordedAt)})`
                : 'не зафиксировано'}
            </div>
            <textarea
              placeholder="Описание дефекта"
              value={defectDescription}
              onChange={(e) => setDefectDescription(e.target.value)}
              required
            />
            <select value={defectSeverity} onChange={(e) => setDefectSeverity(e.target.value as DefectSeverity)}>
              <option value="LOW">Низкая</option>
              <option value="MEDIUM">Средняя</option>
              <option value="HIGH">Высокая</option>
              <option value="CRITICAL">Критическая</option>
            </select>
            <input
              type="number"
              min={0}
              step="0.01"
              placeholder="Рекомендуемый штраф"
              value={recommendedPenalty}
              onChange={(e) => setRecommendedPenalty(e.target.value)}
              required
            />
            <button className="action-btn" type="submit">Создать ведомость</button>
            <button type="button" className="ghost action-btn" onClick={createNoDefectsConclusion} disabled={!defectServiceRequestId}>
              Дефекты не выявлены
            </button>
          </form>
          {equipmentOverview.length === 0 ? (
            <div className="inline-callout inline-callout--warning">
              <strong>Справочник оборудования пуст</strong>
              <p>Обратитесь к менеджеру или выполните миграции базы данных.</p>
            </div>
          ) : null}
          {activeRequests.length === 0 ? <p className="muted">Сейчас нет заявок в работе, доступных для диагностики.</p> : null}

          <div className="summary-chips">
            <span className="status-chip status-new">Новые: {defectSummary.new ?? 0}</span>
            <span className="status-chip status-feedback">Переданы менеджеру: {defectSummary.sentToManager ?? 0}</span>
            <span className="status-chip status-completed">Утверждены: {defectSummary.approved ?? 0}</span>
            <span className="status-chip status-cancelled">Отклонены: {defectSummary.rejected ?? 0}</span>
          </div>

          <div className="toolbar page-toolbar">
            <select value={defectStatusFilter} onChange={(e) => setDefectStatusFilter(e.target.value)}>
              <option value="">Все статусы</option>
              <option value="NEW">Новая</option>
              <option value="SENT_TO_MANAGER">Передана менеджеру</option>
              <option value="APPROVED">Утверждена</option>
              <option value="REJECTED">Отклонена</option>
            </select>
          </div>

          <ul className="list">
            {defects.map((x) => {
              const canEditDefect = x.status === 'NEW' || x.status === 'SENT_TO_MANAGER'
              return (
              <li key={x.id}>
                <div>
                  <p className="row specialist-chip-row">
                    <strong>#{x.id} · {x.equipmentInventoryCode} · {x.equipmentModelName}</strong>
                    <span className={`status-chip ${defectStatusMeta(x.status).cls}`}>{defectStatusMeta(x.status).label}</span>
                    <span className={`status-chip ${defectSeverityChipClass(x.severity)}`}>{defectSeverityRu(x.severity)}</span>
                  </p>
                  <p className="muted">
                    Заявка: {x.serviceRequestId ?? '—'} · Рекомендуемый штраф: {x.recommendedPenalty} BYN · {formatDate(x.createdAt)}
                  </p>
                  <p className="muted">{x.defectDescription}</p>
                </div>
                <div className="row list-actions">
                  {canEditDefect ? (
                    <button type="button" className="ghost action-btn" onClick={() => openDefectEditModal(x)}>
                      Изменить
                    </button>
                  ) : null}
                  {x.status === 'NEW' ? (
                    <button className="action-btn" onClick={() => sendToManager(x.id)}>Передать менеджеру</button>
                  ) : null}
                </div>
              </li>
              )
            })}
          </ul>
        </div>
      ) : null}
    </section>

    {inspectEquipmentRow ? (
      <div
        className="modal-overlay"
        role="presentation"
        onClick={() => setInspectEquipmentRow(null)}
      >
        <div
          className="modal-card"
          role="dialog"
          aria-modal="true"
          aria-labelledby="specialist-inspect-title"
          onClick={(e) => e.stopPropagation()}
        >
          <h3 id="specialist-inspect-title">
            Осмотр: #{inspectEquipmentRow.equipmentId} · {inspectEquipmentRow.inventoryCode} · {inspectEquipmentRow.modelName}
          </h3>
          <p className="muted">Категория: {inspectEquipmentRow.category} · услуга каталога: {inspectEquipmentRow.catalogTitle}</p>
          <form className="form-grid" onSubmit={recordState}>
            <label>
              Оценка состояния: <strong>{inspectConditionScore}/100</strong> · {conditionLabelRuByScore(inspectConditionScore)}
              <input
                type="range"
                min={0}
                max={100}
                step={1}
                value={inspectConditionScore}
                onChange={(e) => setInspectConditionScore(Number(e.target.value))}
              />
              <div className="inspect-slider-labels">
                <span>Аварийное</span>
                <span>Плохое</span>
                <span>Удовл.</span>
                <span>Хорошее</span>
                <span>Отличное</span>
              </div>
            </label>
            <input
              type="number"
              min={0}
              max={100}
              placeholder="Заряд аккумулятора, %"
              value={batteryPercent}
              onChange={(e) => setBatteryPercent(e.target.value)}
            />
            <input
              type="number"
              step="0.1"
              placeholder="Температура, °C"
              value={temperatureCelsius}
              onChange={(e) => setTemperatureCelsius(e.target.value)}
            />
            <input
              type="number"
              min={0}
              placeholder="Прирост наработки (часы)"
              value={operatingHoursDelta}
              onChange={(e) => setOperatingHoursDelta(e.target.value)}
            />
            <textarea placeholder="Комментарий специалиста" value={notes} onChange={(e) => setNotes(e.target.value)} />
            <div className="row">
              <button type="submit">Сохранить осмотр</button>
              <button type="button" className="ghost" onClick={() => setInspectEquipmentRow(null)}>
                Отмена
              </button>
            </div>
          </form>
        </div>
      </div>
    ) : null}

    {defectEditModal ? (
      <div
        className="modal-overlay"
        role="presentation"
        onClick={() => setDefectEditModal(null)}
      >
        <div
          className="modal-card"
          role="dialog"
          aria-modal="true"
          aria-labelledby="specialist-defect-edit-title"
          onClick={(e) => e.stopPropagation()}
        >
          <h3 id="specialist-defect-edit-title">Редактирование ведомости №{defectEditModal.id}</h3>
          <p className="muted">
            {defectEditModal.equipmentInventoryCode} · {defectEditModal.equipmentModelName}
            {defectEditModal.serviceRequestId != null ? ` · заявка №${defectEditModal.serviceRequestId}` : null}
          </p>
          <form className="form-grid" onSubmit={submitDefectEdit}>
            <label>
              Описание
              <textarea
                value={editDefectDescription}
                onChange={(e) => setEditDefectDescription(e.target.value)}
                required
                rows={6}
              />
            </label>
            {editDefectDescription.trim().startsWith('Дефекты не выявлены') ? null : (
              <>
                <label>
                  Степень дефекта
                  <select value={editDefectSeverity} onChange={(e) => setEditDefectSeverity(e.target.value as DefectSeverity)}>
                    <option value="LOW">Низкая</option>
                    <option value="MEDIUM">Средняя</option>
                    <option value="HIGH">Высокая</option>
                    <option value="CRITICAL">Критическая</option>
                  </select>
                </label>
                <label>
                  Рекомендуемый штраф (BYN)
                  <input
                    type="number"
                    min={0}
                    step="0.01"
                    value={editDefectPenalty}
                    onChange={(e) => setEditDefectPenalty(e.target.value)}
                    required
                  />
                </label>
              </>
            )}
            <div className="row">
              <button type="submit">Сохранить</button>
              <button type="button" className="ghost" onClick={() => setDefectEditModal(null)}>
                Отмена
              </button>
            </div>
          </form>
        </div>
      </div>
    ) : null}

    {equipmentDetailModal ? (
      <div
        className="modal-overlay"
        role="presentation"
        onClick={() => {
          if (equipmentDetailLoading) return
          setEquipmentDetailModal(null)
        }}
      >
        <div
          className="modal-card specialist-equipment-detail-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="specialist-equipment-detail-title"
          onClick={(e) => e.stopPropagation()}
        >
          <h3 id="specialist-equipment-detail-title">
            #{equipmentDetailModal.row.equipmentId} · {equipmentDetailModal.row.inventoryCode} · {equipmentDetailModal.row.modelName}
          </h3>
          <p className="muted">
            {equipmentDetailModal.row.catalogTitle} · категория: {equipmentDetailModal.row.category}
          </p>
          <p>
            Накопленный износ (эксплуатация):{' '}
            <strong>{Number(equipmentDetailModal.row.accumulatedWearPercent).toFixed(2)}%</strong>
            {' · '}
            Норма износа: {Number(equipmentDetailModal.row.rentalWearRatePerDay).toFixed(4)}% за сутки аренды
          </p>
          {(() => {
            const wearPercent = Number(equipmentDetailModal.row.accumulatedWearPercent)
            const wearValue = Number.isFinite(wearPercent) ? Math.min(100, Math.max(0, wearPercent)) : 0
            const wearMeta = equipmentHealth(wearValue)
            return (
              <section className="wear-scale-card">
                <div className="wear-scale-head">
                  <strong>Визуальная шкала состояния</strong>
                  <span className={`status-chip ${wearMeta.cls}`}>{wearMeta.label}</span>
                </div>
                <div className="wear-scale-track" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={wearValue}>
                  <div className="wear-scale-markers" aria-hidden>
                    <span style={{ left: '20%' }} />
                    <span style={{ left: '45%' }} />
                    <span style={{ left: '70%' }} />
                  </div>
                  <div className={`wear-scale-fill ${wearMeta.cls}`} style={{ width: `${wearValue}%` }} />
                </div>
                <div className="wear-scale-labels">
                  <span>0%</span>
                  <span>25%</span>
                  <span>50%</span>
                  <span>75%</span>
                  <span>100%</span>
                </div>
                <p className="muted">Текущий износ: {wearValue.toFixed(2)}%</p>
              </section>
            )
          })()}
          <h4 className="specialist-subsection-title">История изменений состояния</h4>
          {equipmentDetailLoading ? <p>Загрузка…</p> : null}
          <div className="equipment-timeline">
            {equipmentDetailModal.timeline.map((ev) => {
              const meta = timelineEntryChip(ev)
              const inspectionWearRaw = ev.inspectionCalculatedWearPercent
              const inspectionWear = inspectionWearRaw == null ? null : Math.max(0, Math.min(100, Number(inspectionWearRaw)))
              const inspectionWearMeta = inspectionWear == null ? null : equipmentHealth(inspectionWear)
              return (
                <article
                  key={`${ev.eventType}-${ev.occurredAt}-${ev.stateHistoryId ?? ''}-${ev.wearLedgerId ?? ''}`}
                  className="timeline-item"
                >
                  <header className="timeline-item-head">
                    <span className={`status-chip ${meta.cls}`}>{meta.chip}</span>
                    <span className="muted">{formatDate(ev.occurredAt)}</span>
                  </header>
                  <h5 className="timeline-item-title">{ev.title}</h5>
                  <div className="timeline-metrics">
                    {inspectionWear != null ? (
                      <>
                        <span className={`status-chip ${inspectionWearMeta?.cls ?? 'status-new'}`}>
                          Износ по осмотру: {inspectionWear.toFixed(1)}%
                        </span>
                        <div className="timeline-wear-track" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={inspectionWear}>
                          <div className={`timeline-wear-fill ${inspectionWearMeta?.cls ?? 'status-new'}`} style={{ width: `${inspectionWear}%` }} />
                        </div>
                      </>
                    ) : null}
                    {ev.rentalWearDelta != null ? (
                      <span className="timeline-metric-chip">Износ аренды: +{Number(ev.rentalWearDelta).toFixed(2)}%</span>
                    ) : null}
                    {ev.defectWearDelta != null ? (
                      <span className="timeline-metric-chip">Износ дефекта: +{Number(ev.defectWearDelta).toFixed(2)}%</span>
                    ) : null}
                    {ev.rentalDays != null ? (
                      <span className="timeline-metric-chip">Суток аренды: {ev.rentalDays}</span>
                    ) : null}
                  </div>
                  <div className="timeline-desc">{ev.description}</div>
                </article>
              )
            })}
          </div>
          {!equipmentDetailLoading && equipmentDetailModal.timeline.length === 0 ? (
            <p className="muted">Записей пока нет.</p>
          ) : null}
          <div className="toolbar specialist-modal-actions">
            <button type="button" className="ghost action-btn" onClick={() => setEquipmentDetailModal(null)} disabled={equipmentDetailLoading}>
              Закрыть
            </button>
          </div>
        </div>
      </div>
    ) : null}
    </>
  )
}

export function App() {
  const { isUser, isManager, isSpecialist } = useRoles()
  const homePath = isUser ? '/services' : isManager ? '/manager' : isSpecialist ? '/specialist' : '/auth'

  return (
    <main className="layout">
      <Header />
      <Routes>
        <Route path="/" element={<Navigate to={homePath} replace />} />
        <Route path="/auth" element={<AuthPage />} />
        <Route path="/auth/callback" element={<OAuthCallbackPage />} />
        <Route path="/services" element={<Protected><ServicesPage /></Protected>} />
        <Route path="/services/:id" element={<Protected><ServiceDetailsPage /></Protected>} />
        <Route
          path="/saved"
          element={<Protected><RoleProtected roles={['USER']}><SavedPage /></RoleProtected></Protected>}
        />
        <Route
          path="/requests"
          element={<Protected><RoleProtected roles={['USER']}><RequestsPage /></RoleProtected></Protected>}
        />
        <Route
          path="/invoices"
          element={<Protected><RoleProtected roles={['USER']}><InvoicesPage /></RoleProtected></Protected>}
        />
        <Route path="/profile" element={<Protected><ProfilePage /></Protected>} />
        <Route
          path="/manager"
          element={<Protected><RoleProtected roles={['MANAGER']}><ManagerPage /></RoleProtected></Protected>}
        />
        <Route
          path="/specialist"
          element={<Protected><RoleProtected roles={['SERVICE_SPECIALIST']}><SpecialistPage /></RoleProtected></Protected>}
        />
        <Route path="*" element={<section className="card"><p>Страница не найдена. <Link to="/services">К каталогу</Link></p></section>} />
      </Routes>
    </main>
  )
}
