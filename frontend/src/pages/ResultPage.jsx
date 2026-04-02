import { useParams } from 'react-router-dom'
import { useEffect, useState } from 'react'
import axios from 'axios'
import ProgressBar from '../components/ProgressBar'
import ReportViewer from '../components/ReportViewer'

export default function ResultPage() {
  const { jobId } = useParams()
  const [status, setStatus] = useState(null)
  const [result, setResult] = useState(null)

  useEffect(() => {
    axios.get(`/api/jobs/${jobId}`).then(r => setStatus(r.data.status))
  }, [jobId])

  useEffect(() => {
    if (status === 'DONE') {
      axios.get(`/api/jobs/${jobId}/result`).then(r => setResult(r.data))
    }
  }, [status, jobId])

  return (
    <div>
      <h2>평가 결과 (Job #{jobId})</h2>
      <ProgressBar jobId={jobId} onDone={() => setStatus('DONE')} />
      {result && <ReportViewer results={result} />}
    </div>
  )
}
