import React, { useState, useEffect } from 'react';
import {
  X,
  Zap,
  AlertTriangle,
  RefreshCw,
  Cpu,
  Info,
  ShieldCheck,
  ShieldAlert,
} from 'lucide-react';
import { api } from '../services/api';

function formatCurrency(val) {
  if (val === null || val === undefined) return '—';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(val);
}

function formatRiskPercent(score) {
  if (score === null || score === undefined) return '—';
  return `${(score * 100).toFixed(2)}%`;
}

export default function TransactionDetailModal({
  transaction,
  onClose,
  onPredictionSuccess,
}) {
  const [isPredicting, setIsPredicting] = useState(false);
  const [error, setError] = useState(null);
  const [localTx, setLocalTx] = useState(transaction);

  // Close on Escape key press
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && !isPredicting) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose, isPredicting]);

  if (!localTx) return null;

  const handleRunPrediction = async () => {
    setIsPredicting(true);
    setError(null);
    try {
      const predResult = await api.runPrediction(localTx.id);
      const updated = {
        ...localTx,
        processingStatus: predResult.processingStatus || 'COMPLETED',
        riskScore: predResult.riskScore,
        severity: predResult.severity,
        isAnomaly: predResult.isAnomaly,
        predictedAt: predResult.predictedAt,
        modelVersion: predResult.modelVersion,
      };
      setLocalTx(updated);
      if (onPredictionSuccess) {
        onPredictionSuccess(updated, predResult);
      }
    } catch (err) {
      setError(err.message || 'Failed to complete ML inference.');
    } finally {
      setIsPredicting(false);
    }
  };

  const hasPrediction = localTx.riskScore !== null && localTx.riskScore !== undefined;
  const isFailed = localTx.processingStatus === 'FAILED';
  const riskScore = localTx.riskScore ?? 0;
  const isModelFraud = Boolean(localTx.isAnomaly);

  let gaugeColor = 'var(--risk-low)';
  if (riskScore >= 0.7) {
    gaugeColor = 'var(--risk-high)';
  } else if (riskScore >= 0.3) {
    gaugeColor = 'var(--risk-medium)';
  }

  return (
    <div
      className="modal-backdrop"
      onClick={() => {
        if (!isPredicting) onClose();
      }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-tx-title"
    >
      <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title-wrap">
            <span id="modal-tx-title" className="modal-title">
              Transaction Audit #{localTx.id}
            </span>
            <span
              className={`badge ${
                localTx.actualFraud ? 'badge-fraud' : 'badge-genuine'
              }`}
            >
              Ground Truth: {localTx.actualFraud ? 'Confirmed Fraud' : 'Genuine'}
            </span>
          </div>
          <button
            className="btn btn-secondary btn-icon"
            onClick={onClose}
            disabled={isPredicting}
            aria-label="Close transaction details"
          >
            <X size={16} />
          </button>
        </div>

        <div className="modal-body">
          {error && (
            <div className="alert-box alert-danger">
              <AlertTriangle size={16} />
              <div style={{ flex: 1 }}>
                <strong>Inference Request Failed:</strong> {error}
              </div>
            </div>
          )}

          {/* Section 1: Ground Truth Dataset Fields */}
          <div>
            <div className="section-group-title" style={{ marginBottom: '8px' }}>
              Historical Ground-Truth Attributes
            </div>
            <div className="detail-grid">
              <div className="detail-item">
                <div className="detail-label">Transaction Amount</div>
                <div className="detail-value mono" style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                  {formatCurrency(localTx.amount)}
                </div>
              </div>

              <div className="detail-item">
                <div className="detail-label">Processing Lifecycle</div>
                <div style={{ marginTop: '3px' }}>
                  <span className={`badge badge-${(localTx.processingStatus || 'pending').toLowerCase()}`}>
                    {localTx.processingStatus || 'PENDING'}
                  </span>
                </div>
              </div>

              <div className="detail-item">
                <div className="detail-label">Dataset Time Offset</div>
                <div className="detail-value mono">
                  {localTx.datasetTime !== null && localTx.datasetTime !== undefined
                    ? `${localTx.datasetTime} seconds`
                    : 'N/A'}
                </div>
              </div>

              <div className="detail-item">
                <div className="detail-label">Database Recorded</div>
                <div className="detail-value mono" style={{ fontSize: '0.8rem' }}>
                  {localTx.createdAt
                    ? new Date(localTx.createdAt).toLocaleString()
                    : 'System Recorded'}
                </div>
              </div>
            </div>
          </div>

          {/* Section 2: Machine Learning Prediction Assessment */}
          <div className="prediction-panel">
            <div className="prediction-panel-header">
              <div className="prediction-panel-title">
                <Cpu size={16} />
                <span>Production Model Assessment (Logistic Regression)</span>
              </div>
              {hasPrediction && (
                <div style={{ display: 'flex', gap: '6px' }}>
                  <span
                    className={`badge ${
                      localTx.severity === 'HIGH'
                        ? 'badge-risk-high'
                        : localTx.severity === 'MEDIUM'
                        ? 'badge-risk-med'
                        : 'badge-risk-low'
                    }`}
                  >
                    Severity: {localTx.severity || 'LOW'}
                  </span>
                </div>
              )}
            </div>

            {hasPrediction ? (
              <div className="gauge-container">
                {/* 1. Probability Score Gauge */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                    Model Fraud Probability Score:
                  </span>
                  <span
                    className="mono"
                    style={{
                      fontSize: '1.4rem',
                      fontWeight: 700,
                      color: gaugeColor,
                    }}
                  >
                    {formatRiskPercent(localTx.riskScore)}
                  </span>
                </div>

                <div className="gauge-bar-track">
                  <div
                    className="gauge-bar-fill"
                    style={{
                      width: `${Math.min(100, Math.max(2, (localTx.riskScore || 0) * 100))}%`,
                      backgroundColor: gaugeColor,
                    }}
                  />
                </div>

                <div className="gauge-labels">
                  <span>0% (Safe)</span>
                  <span>Backend High Tier (70%)</span>
                  <span>Model Threshold (95%)</span>
                </div>

                {/* 2. Distinct Model Decision vs Severity Tier Breakdown */}
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(2, 1fr)',
                    gap: '10px',
                    marginTop: '8px',
                    paddingTop: '10px',
                    borderTop: '1px solid var(--border-subtle)',
                  }}
                >
                  <div style={{ background: 'var(--bg-subtle)', padding: '10px', borderRadius: '6px' }}>
                    <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                      Binary Model Decision (Threshold 0.95)
                    </span>
                    <div style={{ marginTop: '4px' }}>
                      {isModelFraud ? (
                        <span className="badge badge-fraud" style={{ fontSize: '0.75rem' }}>
                          <ShieldAlert size={12} /> POSITIVE: FLAGGED FRAUD
                        </span>
                      ) : (
                        <span className="badge badge-genuine" style={{ fontSize: '0.75rem' }}>
                          <ShieldCheck size={12} /> NEGATIVE: NOT FLAGGED
                        </span>
                      )}
                    </div>
                    <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                      {isModelFraud
                        ? 'Probability >= 0.95 threshold.'
                        : 'Probability < 0.95 threshold.'}
                    </div>
                  </div>

                  <div style={{ background: 'var(--bg-subtle)', padding: '10px', borderRadius: '6px' }}>
                    <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                      Backend Risk Severity Tier
                    </span>
                    <div style={{ marginTop: '4px' }}>
                      <span
                        className={`badge ${
                          localTx.severity === 'HIGH'
                            ? 'badge-risk-high'
                            : localTx.severity === 'MEDIUM'
                            ? 'badge-risk-med'
                            : 'badge-risk-low'
                        }`}
                        style={{ fontSize: '0.75rem' }}
                      >
                        {localTx.severity} SEVERITY
                      </span>
                    </div>
                    <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                      HIGH (&gt;=0.70) | MED (0.30-0.70) | LOW (&lt;0.30)
                    </div>
                  </div>
                </div>

                {/* Explicit Explanation of Discrepancy if Severity is HIGH but isAnomaly is false */}
                {localTx.severity === 'HIGH' && !isModelFraud && (
                  <div
                    style={{
                      background: 'rgba(245, 158, 11, 0.08)',
                      border: '1px solid rgba(245, 158, 11, 0.25)',
                      borderRadius: '6px',
                      padding: '8px 12px',
                      fontSize: '0.75rem',
                      color: 'var(--status-pending-text)',
                      display: 'flex',
                      gap: '8px',
                      alignItems: 'flex-start',
                      marginTop: '4px',
                    }}
                  >
                    <Info size={15} style={{ flexShrink: 0, marginTop: '2px' }} />
                    <div>
                      <strong>Severity vs. Model Decision Distinction:</strong> This transaction is assigned{' '}
                      <strong>HIGH</strong> severity by the backend monitoring rules because its probability (
                      {formatRiskPercent(localTx.riskScore)}) is &gt;= 70%. However, the ML model's binary fraud
                      decision remains <strong>NEGATIVE</strong> because it does not meet the strict 0.95 threshold.
                    </div>
                  </div>
                )}

                {/* Model Metadata */}
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    fontSize: '0.725rem',
                    color: 'var(--text-muted)',
                    paddingTop: '6px',
                  }}
                >
                  <div>
                    Estimator: <span className="mono" style={{ color: 'var(--text-secondary)' }}>LogisticRegression (Balanced)</span>
                  </div>
                  <div>
                    Features: <span className="mono" style={{ color: 'var(--text-secondary)' }}>31 Engineered</span>
                  </div>
                  <div>
                    Decision Threshold: <span className="mono" style={{ color: 'var(--text-secondary)' }}>0.95</span>
                  </div>
                </div>
              </div>
            ) : isFailed ? (
              <div style={{ color: 'var(--risk-high)', fontSize: '0.825rem', lineHeight: 1.4 }}>
                Inference attempt failed. Ensure the Python FastAPI service on port 8000 is running and click <strong>"Retry Inference"</strong> below.
              </div>
            ) : (
              <div style={{ color: 'var(--text-secondary)', fontSize: '0.825rem', lineHeight: 1.4 }}>
                This transaction has not yet been processed by the inference engine.
                Click <strong>"Execute Prediction"</strong> to run inference against the 31 engineered features using the calibrated production model.
              </div>
            )}
          </div>
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose} disabled={isPredicting}>
            Close
          </button>

          <button
            className="btn btn-primary"
            onClick={handleRunPrediction}
            disabled={isPredicting}
          >
            {isPredicting ? (
              <>
                <span className="spinner" />
                <span>Evaluating Features...</span>
              </>
            ) : isFailed ? (
              <>
                <RefreshCw size={14} />
                <span>Retry Inference</span>
              </>
            ) : (
              <>
                <Zap size={14} />
                <span>{hasPrediction ? 'Re-evaluate Prediction' : 'Execute Prediction'}</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
