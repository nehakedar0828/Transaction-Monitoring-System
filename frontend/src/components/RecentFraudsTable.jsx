import React from 'react';
import { ShieldAlert, Eye, Zap, RefreshCw } from 'lucide-react';

function formatCurrency(val) {
  if (val === null || val === undefined) return '—';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(val);
}

export default function RecentFraudsTable({
  recentFrauds,
  loading,
  onSelectTransaction,
  onQuickPredict,
  predictingId,
}) {
  return (
    <div className="content-card">
      <div className="card-header-toolbar">
        <div className="toolbar-top">
          <div className="section-title">
            <ShieldAlert size={16} color="var(--risk-high)" />
            <span>High Priority Alerts: 10 Most Recent Ground-Truth Fraud Cases</span>
          </div>
          <span className="total-badge">
            Endpoint: GET /api/dashboard/recent-frauds
          </span>
        </div>
      </div>

      <div className="table-responsive">
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Time Step</th>
              <th>Amount</th>
              <th>Ground Truth</th>
              <th>Processing Status</th>
              <th>Risk Score &amp; Tier</th>
              <th>Model Decision (&ge;0.95)</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={8}>
                  <div className="loading-box">
                    <span className="spinner spinner-lg" />
                    <span>Loading recent confirmed fraud records...</span>
                  </div>
                </td>
              </tr>
            ) : recentFrauds.length === 0 ? (
              <tr>
                <td colSpan={8}>
                  <div className="empty-box">
                    <span>No confirmed fraud records found.</span>
                  </div>
                </td>
              </tr>
            ) : (
              recentFrauds.map((tx) => {
                const hasPrediction = tx.riskScore !== null && tx.riskScore !== undefined;
                const isThisPredicting = predictingId === tx.id;
                const isFailed = tx.processingStatus === 'FAILED';

                return (
                  <tr key={tx.id}>
                    <td className="mono-cell" style={{ fontWeight: 700, color: 'var(--risk-high)' }}>
                      #{tx.id}
                    </td>
                    <td className="mono-cell" style={{ color: 'var(--text-secondary)' }}>
                      {tx.datasetTime}s
                    </td>
                    <td className="mono-cell" style={{ fontWeight: 600 }}>
                      {formatCurrency(tx.amount)}
                    </td>
                    <td>
                      <span className="badge badge-fraud">
                        Confirmed Fraud
                      </span>
                    </td>
                    <td>
                      <span className={`badge badge-${(tx.processingStatus || 'pending').toLowerCase()}`}>
                        {tx.processingStatus || 'PENDING'}
                      </span>
                    </td>
                    <td>
                      {hasPrediction ? (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <span
                            className={`badge ${
                              tx.severity === 'HIGH'
                                ? 'badge-risk-high'
                                : tx.severity === 'MEDIUM'
                                ? 'badge-risk-med'
                                : 'badge-risk-low'
                            }`}
                          >
                            {tx.severity || 'LOW'}
                          </span>
                          <span className="mono-cell" style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                            {(tx.riskScore * 100).toFixed(1)}%
                          </span>
                        </div>
                      ) : (
                        <span className="badge badge-unpredicted">
                          Not Evaluated
                        </span>
                      )}
                    </td>
                    <td>
                      {hasPrediction ? (
                        tx.isAnomaly ? (
                          <span
                            className="badge badge-risk-high"
                            style={{ fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '4px' }}
                            title="Binary model prediction: Flagged as fraud (prob >= 0.95)"
                          >
                            <ShieldAlert size={12} />
                            Flagged Fraud
                          </span>
                        ) : (
                          <span
                            className="badge badge-processed"
                            style={{ color: 'var(--text-secondary)', background: 'var(--bg-secondary)', border: '1px solid var(--border-color)' }}
                            title="Binary model prediction: Negative / Not flagged as fraud at 0.95 threshold (prob < 0.95)"
                          >
                            Negative (&lt;0.95)
                          </span>
                        )
                      ) : (
                        <span className="badge badge-unpredicted">
                          Not Evaluated
                        </span>
                      )}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '6px' }}>
                        <button
                          className="btn btn-secondary btn-sm"
                          onClick={() => onSelectTransaction(tx)}
                          title="Inspect record"
                        >
                          <Eye size={12} />
                          <span>Inspect</span>
                        </button>
                        <button
                          className="btn btn-primary btn-sm"
                          onClick={() => onQuickPredict(tx.id)}
                          disabled={isThisPredicting || predictingId !== null}
                          title={isFailed ? 'Retry inference' : 'Execute ML prediction'}
                        >
                          {isThisPredicting ? (
                            <span className="spinner" style={{ width: 11, height: 11 }} />
                          ) : isFailed ? (
                            <RefreshCw size={12} />
                          ) : (
                            <Zap size={12} />
                          )}
                          <span>{isFailed ? 'Retry' : hasPrediction ? 'Re-predict' : 'Predict'}</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
