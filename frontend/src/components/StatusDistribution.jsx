import React from 'react';
import { Clock, CheckCircle, AlertOctagon, RefreshCw } from 'lucide-react';

function formatNumber(num) {
  if (num === null || num === undefined) return '0';
  return new Intl.NumberFormat('en-US').format(num);
}

export default function StatusDistribution({ statusSummary, loading }) {
  const counts = statusSummary?.statusCounts || {};

  const pendingCount = counts['PENDING'] || 0;
  const completedCount = counts['COMPLETED'] || 0;
  const failedCount = counts['FAILED'] || 0;
  const processingCount = counts['PROCESSING'] || 0;

  return (
    <div className="section-group">
      <div className="section-group-header">
        <span className="section-group-title">ML Pipeline Processing Status</span>
        <span className="section-group-badge">
          Source: GET /api/dashboard/status
        </span>
      </div>

      <div className="status-grid">
        {/* PENDING */}
        <div className="status-card">
          <div
            className="status-card-icon"
            style={{ background: 'var(--status-pending-bg)', color: 'var(--status-pending-text)' }}
          >
            <Clock size={18} />
          </div>
          <div className="status-card-info">
            <span className="status-card-name">Pending Evaluation</span>
            <span className="status-card-count" style={{ color: 'var(--status-pending-text)' }}>
              {loading ? <span className="spinner" /> : formatNumber(pendingCount)}
            </span>
            <span className="status-card-desc">Awaiting model inference</span>
          </div>
        </div>

        {/* COMPLETED */}
        <div className="status-card">
          <div
            className="status-card-icon"
            style={{ background: 'var(--status-completed-bg)', color: 'var(--status-completed-text)' }}
          >
            <CheckCircle size={18} />
          </div>
          <div className="status-card-info">
            <span className="status-card-name">Inference Completed</span>
            <span className="status-card-count" style={{ color: 'var(--status-completed-text)' }}>
              {loading ? <span className="spinner" /> : formatNumber(completedCount)}
            </span>
            <span className="status-card-desc">Predictions persisted in DB</span>
          </div>
        </div>

        {/* PROCESSING */}
        <div className="status-card">
          <div
            className="status-card-icon"
            style={{ background: 'var(--status-processing-bg)', color: 'var(--status-processing-text)' }}
          >
            <RefreshCw size={18} />
          </div>
          <div className="status-card-info">
            <span className="status-card-name">Processing Active</span>
            <span className="status-card-count" style={{ color: 'var(--status-processing-text)' }}>
              {loading ? <span className="spinner" /> : formatNumber(processingCount)}
            </span>
            <span className="status-card-desc">In-flight transactions</span>
          </div>
        </div>

        {/* FAILED */}
        <div className="status-card">
          <div
            className="status-card-icon"
            style={{ background: 'var(--status-failed-bg)', color: 'var(--status-failed-text)' }}
          >
            <AlertOctagon size={18} />
          </div>
          <div className="status-card-info">
            <span className="status-card-name">Inference Failed</span>
            <span className="status-card-count" style={{ color: 'var(--status-failed-text)' }}>
              {loading ? <span className="spinner" /> : formatNumber(failedCount)}
            </span>
            <span className="status-card-desc">Eligible for manual retry</span>
          </div>
        </div>
      </div>
    </div>
  );
}
