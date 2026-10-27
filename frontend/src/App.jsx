import React, { useState, useEffect, useCallback } from 'react';
import Header from './components/Header';
import MetricCards from './components/MetricCards';
import StatusDistribution from './components/StatusDistribution';
import TransactionTable from './components/TransactionTable';
import RecentFraudsTable from './components/RecentFraudsTable';
import TransactionDetailModal from './components/TransactionDetailModal';
import { useTheme } from './hooks/useTheme';
import { api } from './services/api';
import { AlertTriangle, CheckCircle, List, ShieldAlert } from 'lucide-react';

export default function App() {
  const { theme, toggleTheme } = useTheme();

  // Metrics State
  const [summary, setSummary] = useState(null);
  const [amountSummary, setAmountSummary] = useState(null);
  const [statusSummary, setStatusSummary] = useState(null);
  const [recentFrauds, setRecentFrauds] = useState([]);
  const [loadingMetrics, setLoadingMetrics] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Active Tab: 'all' (All Transactions) vs 'recent-frauds' (Recent Confirmed Frauds)
  const [activeTab, setActiveTab] = useState('all');

  // Transactions Ledger State
  const [transactions, setTransactions] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [sortBy, setSortBy] = useState('id');
  const [direction, setDirection] = useState('asc');
  const [filters, setFilters] = useState({
    fraud: null,
    minAmount: null,
    maxAmount: null,
  });
  const [loadingTable, setLoadingTable] = useState(true);

  // Modal & Prediction State
  const [selectedTransaction, setSelectedTransaction] = useState(null);
  const [predictingId, setPredictingId] = useState(null);
  const [globalError, setGlobalError] = useState(null);
  const [successToast, setSuccessToast] = useState(null);

  const showToast = (message) => {
    setSuccessToast(message);
    setTimeout(() => {
      setSuccessToast(null);
    }, 5000);
  };

  // Fetch Summary Metrics
  const fetchMetrics = useCallback(async () => {
    try {
      const [sumRes, amtRes, statRes, recentRes] = await Promise.allSettled([
        api.getDashboardSummary(),
        api.getAmountSummary(),
        api.getStatusSummary(),
        api.getRecentFrauds(),
      ]);

      if (sumRes.status === 'fulfilled') setSummary(sumRes.value);
      if (amtRes.status === 'fulfilled') setAmountSummary(amtRes.value);
      if (statRes.status === 'fulfilled') setStatusSummary(statRes.value);
      if (recentRes.status === 'fulfilled') setRecentFrauds(recentRes.value);
    } catch (err) {
      console.error('Error querying dashboard metrics:', err);
    } finally {
      setLoadingMetrics(false);
    }
  }, []);

  // Fetch Paginated Transactions
  const fetchTransactions = useCallback(async () => {
    setLoadingTable(true);
    setGlobalError(null);
    try {
      const data = await api.getTransactions({
        page: currentPage,
        size: pageSize,
        sortBy,
        direction,
        fraud: filters.fraud,
        minAmount: filters.minAmount,
        maxAmount: filters.maxAmount,
      });

      setTransactions(data.content || []);
      setTotalElements(data.totalElements || 0);
      setTotalPages(data.totalPages || 0);
    } catch (err) {
      setGlobalError(err.message || 'Unable to retrieve transactions from backend service.');
      setTransactions([]);
    } finally {
      setLoadingTable(false);
    }
  }, [currentPage, pageSize, sortBy, direction, filters]);

  // Initial Data Load
  useEffect(() => {
    fetchMetrics();
  }, [fetchMetrics]);

  useEffect(() => {
    fetchTransactions();
  }, [fetchTransactions]);

  // Manual Dashboard Refresh
  const handleFullRefresh = async () => {
    setIsRefreshing(true);
    await Promise.all([fetchMetrics(), fetchTransactions()]);
    setIsRefreshing(false);
    showToast('Dashboard data refreshed.');
  };

  // Table Sorting & Filters
  const handleSortChange = (newSortBy) => {
    if (sortBy === newSortBy) {
      setDirection(direction === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(newSortBy);
      setDirection('asc');
    }
    setCurrentPage(0);
  };

  const handleFilterChange = (newFilters) => {
    setFilters((prev) => ({ ...prev, ...newFilters }));
    setCurrentPage(0);
  };

  const handleResetFilters = () => {
    setFilters({
      fraud: null,
      minAmount: null,
      maxAmount: null,
    });
    setSortBy('id');
    setDirection('asc');
    setCurrentPage(0);
  };

  // Select Transaction for Inspection
  const handleSelectTransaction = async (target) => {
    if (typeof target === 'object' && target !== null) {
      setSelectedTransaction(target);
    } else {
      setLoadingTable(true);
      try {
        const tx = await api.getTransactionById(target);
        setSelectedTransaction(tx);
      } catch (err) {
        setGlobalError(`Transaction #${target} not found: ${err.message}`);
      } finally {
        setLoadingTable(false);
      }
    }
  };

  // Execute ML Prediction for Single Transaction
  const handleRunQuickPredict = async (txId) => {
    if (predictingId !== null) return; // Prevent concurrent requests
    setPredictingId(txId);
    setGlobalError(null);

    try {
      const predResult = await api.runPrediction(txId);

      // Update table state
      setTransactions((prev) =>
        prev.map((t) =>
          t.id === txId
            ? {
                ...t,
                processingStatus: predResult.processingStatus || 'COMPLETED',
                riskScore: predResult.riskScore,
                severity: predResult.severity,
              }
            : t
        )
      );

      // Update recent frauds state if present
      setRecentFrauds((prev) =>
        prev.map((t) =>
          t.id === txId
            ? {
                ...t,
                processingStatus: predResult.processingStatus || 'COMPLETED',
                riskScore: predResult.riskScore,
                severity: predResult.severity,
              }
            : t
        )
      );

      // If modal is currently viewing this transaction, sync it
      if (selectedTransaction && selectedTransaction.id === txId) {
        setSelectedTransaction((prev) => ({
          ...prev,
          processingStatus: predResult.processingStatus || 'COMPLETED',
          riskScore: predResult.riskScore,
          severity: predResult.severity,
          isAnomaly: predResult.isAnomaly,
          predictedAt: predResult.predictedAt,
        }));
      }

      // Re-fetch status breakdown metrics to reflect updated state
      api.getStatusSummary().then((s) => setStatusSummary(s)).catch(() => {});

      const scorePct = (predResult.riskScore * 100).toFixed(2);
      showToast(
        `Inference finished for #${txId}: Risk Score ${scorePct}% (${predResult.severity} Severity)`
      );
    } catch (err) {
      setGlobalError(`Inference request failed for #${txId}: ${err.message}`);
    } finally {
      setPredictingId(null);
    }
  };

  return (
    <div className="app-container">
      {/* Header with theme toggle and live refresh */}
      <Header
        onRefresh={handleFullRefresh}
        isRefreshing={isRefreshing}
        theme={theme}
        onToggleTheme={toggleTheme}
      />

      {/* Global Alert Banners */}
      {globalError && (
        <div className="alert-box alert-danger" role="alert">
          <AlertTriangle size={16} />
          <div style={{ flex: 1 }}>{globalError}</div>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => setGlobalError(null)}
          >
            Dismiss
          </button>
        </div>
      )}

      {successToast && (
        <div className="alert-box alert-success" role="status">
          <CheckCircle size={16} />
          <div style={{ flex: 1 }}>{successToast}</div>
        </div>
      )}

      {/* Ground Truth Dataset KPI Metrics */}
      <MetricCards
        summary={summary}
        amountSummary={amountSummary}
        loading={loadingMetrics}
      />

      {/* ML Pipeline Processing Status Breakdown */}
      <StatusDistribution
        statusSummary={statusSummary}
        loading={loadingMetrics}
      />

      {/* Ledger Navigation Tabs */}
      <div className="tabs-container">
        <button
          className={`tab-btn ${activeTab === 'all' ? 'active' : ''}`}
          onClick={() => setActiveTab('all')}
        >
          <List size={15} />
          <span>All Transactions Ledger</span>
        </button>

        <button
          className={`tab-btn ${activeTab === 'recent-frauds' ? 'active' : ''}`}
          onClick={() => setActiveTab('recent-frauds')}
        >
          <ShieldAlert size={15} />
          <span>Confirmed Fraud Cases ({recentFrauds.length})</span>
        </button>
      </div>

      {/* Active Tab Panel */}
      {activeTab === 'all' ? (
        <TransactionTable
          transactions={transactions}
          totalElements={totalElements}
          totalPages={totalPages}
          currentPage={currentPage}
          pageSize={pageSize}
          sortBy={sortBy}
          direction={direction}
          loading={loadingTable}
          filters={filters}
          onPageChange={setCurrentPage}
          onPageSizeChange={(newSize) => {
            setPageSize(newSize);
            setCurrentPage(0);
          }}
          onSortChange={handleSortChange}
          onFilterChange={handleFilterChange}
          onResetFilters={handleResetFilters}
          onSelectTransaction={handleSelectTransaction}
          onQuickPredict={handleRunQuickPredict}
          predictingId={predictingId}
        />
      ) : (
        <RecentFraudsTable
          recentFrauds={recentFrauds}
          loading={loadingMetrics}
          onSelectTransaction={handleSelectTransaction}
          onQuickPredict={handleRunQuickPredict}
          predictingId={predictingId}
        />
      )}

      {/* Detail Inspection Modal */}
      {selectedTransaction && (
        <TransactionDetailModal
          transaction={selectedTransaction}
          onClose={() => setSelectedTransaction(null)}
          onPredictionSuccess={(updatedTx, predResult) => {
            setTransactions((prev) =>
              prev.map((t) => (t.id === updatedTx.id ? updatedTx : t))
            );
            setRecentFrauds((prev) =>
              prev.map((t) => (t.id === updatedTx.id ? updatedTx : t))
            );
            api.getStatusSummary().then((s) => setStatusSummary(s)).catch(() => {});
            const scorePct = (predResult.riskScore * 100).toFixed(2);
            showToast(
              `ML Prediction recorded for #${updatedTx.id}: Score ${scorePct}% (${predResult.severity})`
            );
          }}
        />
      )}
    </div>
  );
}
