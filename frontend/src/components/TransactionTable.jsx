import React, { useState } from 'react';
import {
  Search,
  Filter,
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  Eye,
  Zap,
  RotateCcw,
  AlertCircle,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
} from 'lucide-react';

function formatCurrency(val) {
  if (val === null || val === undefined) return '—';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(val);
}

function formatNumber(num) {
  if (num === null || num === undefined) return '0';
  return new Intl.NumberFormat('en-US').format(num);
}

export default function TransactionTable({
  transactions,
  totalElements,
  totalPages,
  currentPage,
  pageSize,
  sortBy,
  direction,
  loading,
  filters,
  onPageChange,
  onPageSizeChange,
  onSortChange,
  onFilterChange,
  onResetFilters,
  onSelectTransaction,
  onQuickPredict,
  predictingId,
}) {
  const [localSearchId, setLocalSearchId] = useState('');
  const [localMinAmount, setLocalMinAmount] = useState(filters.minAmount || '');
  const [localMaxAmount, setLocalMaxAmount] = useState(filters.maxAmount || '');
  const [searchError, setSearchError] = useState('');

  const handleApplyFilters = (e) => {
    e.preventDefault();
    onFilterChange({
      minAmount: localMinAmount || null,
      maxAmount: localMaxAmount || null,
    });
  };

  const handleDirectIdSearch = (e) => {
    e.preventDefault();
    setSearchError('');
    const trimmed = localSearchId.trim();
    if (!trimmed) return;
    const num = parseInt(trimmed, 10);
    if (isNaN(num) || num <= 0) {
      setSearchError('Please enter a valid positive Transaction ID');
      return;
    }
    onSelectTransaction(num);
  };

  const renderSortIndicator = (column) => {
    if (sortBy !== column) {
      return <ArrowUpDown size={11} style={{ opacity: 0.35, marginLeft: 4 }} />;
    }
    return (
      <span style={{ color: 'var(--accent-primary)', marginLeft: 4, fontWeight: 700 }}>
        {direction === 'asc' ? '▲' : '▼'}
      </span>
    );
  };

  return (
    <div className="content-card">
      {/* Card Header & Toolbar */}
      <div className="card-header-toolbar">
        <div className="toolbar-top">
          <div className="section-title">
            <span>Transaction Ledger</span>
            <span className="total-badge">
              {formatNumber(totalElements)} Total Records
            </span>
          </div>

          {/* Quick Direct ID Lookup */}
          <form onSubmit={handleDirectIdSearch} style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            <div className="input-group">
              <Search size={14} />
              <input
                type="text"
                placeholder="Find by ID..."
                value={localSearchId}
                onChange={(e) => {
                  setLocalSearchId(e.target.value);
                  setSearchError('');
                }}
                style={{ width: '110px' }}
                aria-label="Direct transaction ID search"
              />
            </div>
            <button type="submit" className="btn btn-secondary btn-sm">
              Inspect ID
            </button>
          </form>
        </div>

        {searchError && (
          <div style={{ color: 'var(--risk-high)', fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: '4px' }}>
            <AlertCircle size={14} /> {searchError}
          </div>
        )}

        {/* Filters and Search Bar */}
        <form onSubmit={handleApplyFilters} className="filters-bar">
          {/* Ground Truth Filter */}
          <div className="input-group">
            <Filter size={14} />
            <select
              value={filters.fraud === null || filters.fraud === undefined ? '' : filters.fraud}
              onChange={(e) => {
                const val = e.target.value === '' ? null : e.target.value === 'true';
                onFilterChange({ fraud: val });
              }}
              aria-label="Filter by ground truth fraud status"
            >
              <option value="">Ground Truth: All</option>
              <option value="false">Genuine Only</option>
              <option value="true">Confirmed Fraud Only</option>
            </select>
          </div>

          {/* Min Amount */}
          <div className="input-group">
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Min $</span>
            <input
              type="number"
              step="0.01"
              min="0"
              placeholder="0.00"
              value={localMinAmount}
              onChange={(e) => setLocalMinAmount(e.target.value)}
              style={{ width: '80px' }}
              aria-label="Minimum amount filter"
            />
          </div>

          {/* Max Amount */}
          <div className="input-group">
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Max $</span>
            <input
              type="number"
              step="0.01"
              min="0"
              placeholder="10000.00"
              value={localMaxAmount}
              onChange={(e) => setLocalMaxAmount(e.target.value)}
              style={{ width: '90px' }}
              aria-label="Maximum amount filter"
            />
          </div>

          <button type="submit" className="btn btn-primary btn-sm">
            Apply
          </button>

          {(localMinAmount || localMaxAmount || filters.fraud !== null) && (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => {
                setLocalMinAmount('');
                setLocalMaxAmount('');
                onResetFilters();
              }}
            >
              <RotateCcw size={12} />
              Reset
            </button>
          )}

          {/* Page Size Selector */}
          <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Rows:</span>
            <div className="input-group" style={{ padding: '3px 6px' }}>
              <select
                value={pageSize}
                onChange={(e) => onPageSizeChange(Number(e.target.value))}
                aria-label="Select rows per page"
              >
                <option value={10}>10</option>
                <option value={25}>25</option>
                <option value={50}>50</option>
                <option value={100}>100</option>
              </select>
            </div>
          </div>
        </form>
      </div>

      {/* Table Content */}
      <div className="table-responsive">
        <table className="data-table">
          <thead>
            <tr>
              <th className="sortable" onClick={() => onSortChange('id')}>
                ID {renderSortIndicator('id')}
              </th>
              <th className="sortable" onClick={() => onSortChange('datasetTime')}>
                Time Step {renderSortIndicator('datasetTime')}
              </th>
              <th className="sortable" onClick={() => onSortChange('amount')}>
                Amount {renderSortIndicator('amount')}
              </th>
              <th>Ground Truth</th>
              <th>Processing Status</th>
              <th>Model Decision (≥0.95)</th>
              <th>Risk Score &amp; Tier</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={8}>
                  <div className="loading-box">
                    <span className="spinner spinner-lg" />
                    <span>Loading transaction ledger...</span>
                  </div>
                </td>
              </tr>
            ) : transactions.length === 0 ? (
              <tr>
                <td colSpan={8}>
                  <div className="empty-box">
                    <div className="empty-box-icon">
                      <Search size={20} />
                    </div>
                    <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                      No Transactions Match Filters
                    </div>
                    <div style={{ fontSize: '0.775rem' }}>
                      Try broadening your amount range or clearing the ground-truth filter.
                    </div>
                  </div>
                </td>
              </tr>
            ) : (
              transactions.map((tx) => {
                const hasPrediction = tx.riskScore !== null && tx.riskScore !== undefined;
                const isThisPredicting = predictingId === tx.id;
                const isFailed = tx.processingStatus === 'FAILED';
                const isModelFraud = Boolean(tx.isAnomaly);

                return (
                  <tr key={tx.id}>
                    {/* ID */}
                    <td className="mono-cell" style={{ fontWeight: 600 }}>
                      #{tx.id}
                    </td>

                    {/* Dataset Time */}
                    <td className="mono-cell" style={{ color: 'var(--text-secondary)' }}>
                      {tx.datasetTime !== null && tx.datasetTime !== undefined ? `${tx.datasetTime}s` : '—'}
                    </td>

                    {/* Amount */}
                    <td className="mono-cell" style={{ fontWeight: 600 }}>
                      {formatCurrency(tx.amount)}
                    </td>

                    {/* Ground Truth Label */}
                    <td>
                      <span className={`badge ${tx.actualFraud ? 'badge-fraud' : 'badge-genuine'}`}>
                        {tx.actualFraud ? 'Fraud' : 'Genuine'}
                      </span>
                    </td>

                    {/* Processing Status */}
                    <td>
                      <span className={`badge badge-${(tx.processingStatus || 'pending').toLowerCase()}`}>
                        {tx.processingStatus || 'PENDING'}
                      </span>
                    </td>

                    {/* Model Binary Decision (Threshold 0.95) */}
                    <td>
                      {hasPrediction ? (
                        isModelFraud ? (
                          <span className="badge badge-fraud">
                            <ShieldAlert size={11} /> Flagged Fraud
                          </span>
                        ) : (
                          <span className="badge badge-genuine">
                            <ShieldCheck size={11} /> Negative (&lt;0.95)
                          </span>
                        )
                      ) : (
                        <span className="badge badge-unpredicted">
                          Not Evaluated
                        </span>
                      )}
                    </td>

                    {/* Risk Score & Severity Tier */}
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
                            {tx.severity}
                          </span>
                          <span className="mono-cell" style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                            {(tx.riskScore * 100).toFixed(1)}%
                          </span>
                        </div>
                      ) : (
                        <span className="mono-cell" style={{ color: 'var(--text-muted)' }}>
                          —
                        </span>
                      )}
                    </td>

                    {/* Row Actions */}
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '6px' }}>
                        <button
                          className="btn btn-secondary btn-sm"
                          onClick={() => onSelectTransaction(tx)}
                          title="Inspect transaction parameters"
                        >
                          <Eye size={12} />
                          <span>Inspect</span>
                        </button>

                        <button
                          className="btn btn-primary btn-sm"
                          onClick={() => onQuickPredict(tx.id)}
                          disabled={isThisPredicting || predictingId !== null}
                          title={
                            isFailed
                              ? 'Retry failed inference'
                              : hasPrediction
                              ? 'Re-evaluate ML prediction'
                              : 'Execute single ML fraud prediction'
                          }
                        >
                          {isThisPredicting ? (
                            <>
                              <span className="spinner" style={{ width: 11, height: 11 }} />
                              <span>Evaluating...</span>
                            </>
                          ) : isFailed ? (
                            <>
                              <RefreshCw size={12} />
                              <span>Retry</span>
                            </>
                          ) : (
                            <>
                              <Zap size={12} />
                              <span>{hasPrediction ? 'Re-predict' : 'Predict'}</span>
                            </>
                          )}
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

      {/* Pagination Footer */}
      <div className="pagination-bar">
        <div className="pagination-info">
          Page <span className="page-num">{currentPage + 1}</span> of{' '}
          <span className="page-num">{Math.max(1, totalPages)}</span> (Total{' '}
          {formatNumber(totalElements)} transactions)
        </div>

        <div className="pagination-controls">
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => onPageChange(0)}
            disabled={currentPage === 0 || loading}
            title="First page"
          >
            « First
          </button>

          <button
            className="btn btn-secondary btn-sm"
            onClick={() => onPageChange(currentPage - 1)}
            disabled={currentPage === 0 || loading}
            title="Previous page"
          >
            <ChevronLeft size={14} /> Prev
          </button>

          <button
            className="btn btn-secondary btn-sm"
            onClick={() => onPageChange(currentPage + 1)}
            disabled={currentPage >= totalPages - 1 || loading}
            title="Next page"
          >
            Next <ChevronRight size={14} />
          </button>

          <button
            className="btn btn-secondary btn-sm"
            onClick={() => onPageChange(totalPages - 1)}
            disabled={currentPage >= totalPages - 1 || loading}
            title="Last page"
          >
            Last »
          </button>
        </div>
      </div>
    </div>
  );
}
