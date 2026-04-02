import { useEffect, useState } from 'react'

export default function ProgressBar({ jobId, onDone }) {
  const [status, setStatus] = useState({ progress: 0, status: 'PENDING' })

  useEffect(() => {
    const source = new EventSource(`/api/jobs/${jobId}/stream`)

    source.onmessage = (e) => {
      const data = JSON.parse(e.data)
      setStatus(data)
      if (data.status === 'DONE' || data.status === 'FAILED') {
        source.close()
        if (data.status === 'DONE' && onDone) onDone()
      }
    }

    source.onerror = () => source.close()
    return () => source.close()
  }, [jobId])

  const statusLabel = {
    PENDING:           '대기 중',
    EVALUATING:        '데이터 평가 중',
    SCORING:           '점수 계산 중',
    GENERATING_REPORT: '보고서 생성 중',
    DONE:              '완료',
    FAILED:            '실패',
  }[status.status] ?? status.status

  return (
    <div style={{ marginBottom: 24 }}>
      <div style={{ marginBottom: 6 }}>{statusLabel} — {status.progress}%</div>
      <div style={{ background: '#eee', borderRadius: 4, height: 8 }}>
        <div style={{
          width: `${status.progress}%`,
          background: status.status === 'FAILED' ? '#e53e3e' : '#3182ce',
          height: '100%',
          borderRadius: 4,
          transition: 'width 0.5s',
        }} />
      </div>
    </div>
  )
}
