export default function ReportViewer({ results }) {
  return (
    <div>
      <h3>평가 항목별 점수</h3>
      <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 24 }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #ddd' }}>
            <th style={{ textAlign: 'left', padding: 8 }}>기준</th>
            <th style={{ textAlign: 'right', padding: 8 }}>점수</th>
            <th style={{ textAlign: 'right', padding: 8 }}>가중치</th>
            <th style={{ textAlign: 'left', padding: 8 }}>설명</th>
          </tr>
        </thead>
        <tbody>
          {results.map(r => (
            <tr key={r.criteriaName} style={{ borderBottom: '1px solid #eee' }}>
              <td style={{ padding: 8 }}>{r.criteriaName}</td>
              <td style={{ textAlign: 'right', padding: 8 }}>{r.score}</td>
              <td style={{ textAlign: 'right', padding: 8 }}>{r.weight}</td>
              <td style={{ padding: 8 }}>{r.detail}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
