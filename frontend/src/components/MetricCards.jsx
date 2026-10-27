import React from 'react';
import { Database, ShieldCheck, ShieldAlert, Percent } from 'lucide-react';

function formatNumber(num) {
  if (num === null || num === undefined) return '—';
  return new Intl.NumberFormat('en-US').format(num);
}

function formatCurrency(val) {
  if (val === null || val === undefined) return '—';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 2,
  }).format(val);
}

export default function MetricCards({ summary, amountSummary, loading }) {
  const totalTx = summary?.totalTransactions;
  const fraudTx = summary?.fraudTransactions;
  const genuineTx = summary?.genuineTransactions;
  const fraudPercent = summary?.fraudPercentage;

  const totalAmount = amountSummary?.totalAmount;
  const fraudAmount = amountSummary?.fraudAmount;
  const genuineAmount = amountSummary?.genuineAmount;

  return (
    <div className="section-group">
      <div className="section-group-header">
        <span className="section-group-title">Dataset Ground Truth Metrics</span>
        <span className="section-group-badge">
          Source: PostgreSQL Database Audit
        </span>
      </div>

      <div className="metrics-grid">
        {/* 1. Total Transactions */}
        <div className="metric-card">
          <div className="metric-header">
            <span className="metric-label">Total Transactions</span>
            <div className="metric-icon-wrap">
              <Database size={15} />
            </div>
          </div>
          <div className="metric-value">
            {loading ? <span className="spinner" /> : formatNumber(totalTx)}
          </div>
          <div className="metric-subtext">
            <span>Gross Volume:</span>
            <strong className="mono-cell">{formatCurrency(totalAmount)}</strong>
          </div>
        </div>

        {/* 2. Genuine Transactions */}
        <div className="metric-card">
          <div className="metric-header">
            <span className="metric-label">Genuine Transactions</span>
            <div className="metric-icon-wrap" style={{ color: 'var(--risk-low)' }}>
              <ShieldCheck size={15} />
            </div>
          </div>
          <div className="metric-value" style={{ color: 'var(--risk-low)' }}>
            {loading ? <span className="spinner" /> : formatNumber(genuineTx)}
          </div>
          <div className="metric-subtext">
            <span>Genuine Volume:</span>
            <strong className="mono-cell">{formatCurrency(genuineAmount)}</strong>
          </div>
        </div>

        {/* 3. Confirmed Fraud Cases */}
        <div className="metric-card">
          <div className="metric-header">
            <span className="metric-label">Confirmed Fraud Cases</span>
            <div className="metric-icon-wrap" style={{ color: 'var(--risk-high)' }}>
              <ShieldAlert size={15} />
            </div>
          </div>
          <div className="metric-value" style={{ color: 'var(--risk-high)' }}>
            {loading ? <span className="spinner" /> : formatNumber(fraudTx)}
          </div>
          <div className="metric-subtext">
            <span>Fraudulent Volume:</span>
            <strong className="mono-cell">{formatCurrency(fraudAmount)}</strong>
          </div>
        </div>

        {/* 4. Ground-Truth Fraud Rate */}
        <div className="metric-card">
          <div className="metric-header">
            <span className="metric-label">Fraud Incident Rate</span>
            <div className="metric-icon-wrap">
              <Percent size={15} />
            </div>
          </div>
          <div className="metric-value">
            {loading ? (
              <span className="spinner" />
            ) : (
              `${(fraudPercent ?? 0).toFixed(3)}%`
            )}
          </div>
          <div className="metric-subtext">
            <span>Ground Truth:</span>
            <span className="mono-cell">{formatNumber(fraudTx)} / {formatNumber(totalTx)}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
