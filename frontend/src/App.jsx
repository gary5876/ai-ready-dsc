import { Routes, Route, Link } from 'react-router-dom'
import UploadPage from './pages/UploadPage'
import JobListPage from './pages/JobListPage'
import ResultPage from './pages/ResultPage'

export default function App() {
  return (
    <div style={{ fontFamily: 'sans-serif', maxWidth: 900, margin: '0 auto', padding: 24 }}>
      <nav style={{ marginBottom: 24, borderBottom: '1px solid #eee', paddingBottom: 12 }}>
        <Link to="/" style={{ marginRight: 16 }}>데이터셋 업로드</Link>
        <Link to="/jobs">작업 목록</Link>
      </nav>
      <Routes>
        <Route path="/" element={<UploadPage />} />
        <Route path="/jobs" element={<JobListPage />} />
        <Route path="/jobs/:jobId/result" element={<ResultPage />} />
      </Routes>
    </div>
  )
}
