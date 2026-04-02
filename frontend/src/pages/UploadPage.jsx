import { useState } from 'react'
import axios from 'axios'
import { useNavigate } from 'react-router-dom'

export default function UploadPage() {
  const [file, setFile] = useState(null)
  const [name, setName] = useState('')
  const [userId] = useState(1)  // TODO: 인증 구현 후 교체
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleUpload = async (e) => {
    e.preventDefault()
    if (!file || !name) return

    setLoading(true)
    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('name', name)
      formData.append('userId', userId)

      const { data: dataset } = await axios.post('/api/datasets/upload', formData)

      const { data: job } = await axios.post('/api/jobs', {
        datasetId: dataset.id,
        userId,
      })

      navigate(`/jobs/${job.jobId}/result`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <h2>데이터셋 업로드</h2>
      <form onSubmit={handleUpload}>
        <div style={{ marginBottom: 12 }}>
          <label>데이터셋 이름</label><br />
          <input value={name} onChange={e => setName(e.target.value)} required style={{ width: 300 }} />
        </div>
        <div style={{ marginBottom: 12 }}>
          <label>CSV 파일</label><br />
          <input type="file" accept=".csv" onChange={e => setFile(e.target.files[0])} required />
        </div>
        <button type="submit" disabled={loading}>
          {loading ? '업로드 중...' : '업로드 및 평가 시작'}
        </button>
      </form>
    </div>
  )
}
