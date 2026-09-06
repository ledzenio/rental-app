import { useNoticeStore, NOTICE_TITLE, type NoticeVariant } from './noticeStore'

function NoticeGlyph({ variant }: { variant: NoticeVariant }) {
  switch (variant) {
    case 'success':
      return <span className="notice-glyph notice-glyph--success">✓</span>
    case 'error':
      return <span className="notice-glyph notice-glyph--error">!</span>
    case 'warning':
      return <span className="notice-glyph notice-glyph--warning">▲</span>
    default:
      return <span className="notice-glyph notice-glyph--info">i</span>
  }
}

export function NoticeHost() {
  const notices = useNoticeStore((s) => s.notices)
  const dismiss = useNoticeStore((s) => s.dismiss)

  if (notices.length === 0) return null

  return (
    <div className="notice-host" aria-label="Уведомления">
      {notices.map((n) => (
        <div
          key={n.id}
          className={`notice-item notice-item--${n.variant}`}
          role={n.variant === 'error' ? 'alert' : 'status'}
        >
          <div className="notice-item__icon" aria-hidden>
            <NoticeGlyph variant={n.variant} />
          </div>
          <div className="notice-item__body">
            <div className="notice-item__title">{NOTICE_TITLE[n.variant]}</div>
            <div className="notice-item__message">{n.message}</div>
          </div>
          <button
            type="button"
            className="notice-item__close"
            onClick={() => dismiss(n.id)}
            aria-label="Закрыть уведомление"
          >
            ×
          </button>
        </div>
      ))}
    </div>
  )
}
