import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'

export default function JobListPage() {
  const [jobs, setJobs] = useState([])
  const userId = 1  // TODO: 인증 구현 후 교체

  useEffect(() => {
    axios.get(`/api/jobs?userId=${userId}`).then(r => setJobs(r.data))
  }, [])

  return (
    <div>
      <h2>평가 작업 목록</h2>
      {jobs.length === 0 ? (
        <p>작업이 없습니다.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #ddd' }}>
              <th style={{ textAlign: 'left', padding: 8 }}>Job ID</th>
              <th style={{ textAlign: 'left', padding: 8 }}>상태</th>
              <th style={{ textAlign: 'left', padding: 8 }}>진행률</th>
              <th style={{ textAlign: 'left', padding: 8 }}>결과</th>
            </tr>
          </thead>
          <tbody>
            {jobs.map(job => (
              <tr key={job.jobId} style={{ borderBottom: '1px solid #eee' }}>
                <td style={{ padding: 8 }}>{job.jobId}</td>
                <td style={{ padding: 8 }}>{job.status}</td>
                <td style={{ padding: 8 }}>{job.progress}%</td>
                <td style={{ padding: 8 }}>
                  {job.status === 'DONE' && (
                    <Link to={`/jobs/${job.jobId}/result`}>결과 보기</Link>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
