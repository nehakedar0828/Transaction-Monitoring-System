/**
 * API Service for Transaction Monitoring Backend
 * Connects directly to Spring Boot backend REST endpoints.
 */

const BASE_URL = import.meta.env.VITE_API_URL || '';

async function handleResponse(response) {
  if (!response.ok) {
    let errorDetail = '';
    try {
      const errJson = await response.json();
      errorDetail = errJson.message || errJson.error || JSON.stringify(errJson);
    } catch {
      errorDetail = await response.text();
    }
    const errorMsg = errorDetail || `Request failed with status ${response.status}`;
    const error = new Error(errorMsg);
    error.status = response.status;
    throw error;
  }
  return response.json();
}

export const api = {
  /**
   * Fetch high-level transaction count and fraud percentage
   * GET /api/dashboard/summary
   */
  async getDashboardSummary() {
    const res = await fetch(`${BASE_URL}/api/dashboard/summary`);
    return handleResponse(res);
  },

  /**
   * Fetch aggregate amount breakdown (total, genuine, fraudulent)
   * GET /api/dashboard/amount-summary
   */
  async getAmountSummary() {
    const res = await fetch(`${BASE_URL}/api/dashboard/amount-summary`);
    return handleResponse(res);
  },

  /**
   * Fetch transaction processing status counts (PENDING, COMPLETED, FAILED, PROCESSING)
   * GET /api/dashboard/status
   */
  async getStatusSummary() {
    const res = await fetch(`${BASE_URL}/api/dashboard/status`);
    return handleResponse(res);
  },

  /**
   * Fetch the 10 most recent fraudulent transactions
   * GET /api/dashboard/recent-frauds
   */
  async getRecentFrauds() {
    const res = await fetch(`${BASE_URL}/api/dashboard/recent-frauds`);
    return handleResponse(res);
  },

  /**
   * Fetch paginated transactions with optional sorting, search amount range, and fraud filtering
   * Uses /api/transactions/search when search filters exist, or /api/transactions for standard paging
   */
  async getTransactions({
    page = 0,
    size = 10,
    sortBy = 'id',
    direction = 'asc',
    fraud = null,
    minAmount = null,
    maxAmount = null,
  } = {}) {
    const params = new URLSearchParams();
    params.set('page', page.toString());
    params.set('size', size.toString());
    params.set('sortBy', sortBy);
    params.set('direction', direction);

    if (fraud !== null && fraud !== undefined && fraud !== '') {
      params.set('fraud', fraud.toString());
    }

    const hasAmountFilter = (minAmount !== null && minAmount !== '') || (maxAmount !== null && maxAmount !== '');

    if (hasAmountFilter) {
      if (minAmount !== null && minAmount !== '') params.set('minAmount', minAmount.toString());
      if (maxAmount !== null && maxAmount !== '') params.set('maxAmount', maxAmount.toString());
      const res = await fetch(`${BASE_URL}/api/transactions/search?${params.toString()}`);
      return handleResponse(res);
    }

    const res = await fetch(`${BASE_URL}/api/transactions?${params.toString()}`);
    return handleResponse(res);
  },

  /**
   * Fetch a single transaction by ID
   * GET /api/transactions/{id}
   */
  async getTransactionById(id) {
    const res = await fetch(`${BASE_URL}/api/transactions/${id}`);
    return handleResponse(res);
  },

  /**
   * Run ML inference prediction for a single transaction
   * Evaluates features, persists Prediction entity, and transitions status to COMPLETED
   * POST /api/fraud/predict/{transactionId}
   */
  async runPrediction(transactionId) {
    const res = await fetch(`${BASE_URL}/api/fraud/predict/${transactionId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    return handleResponse(res);
  },
};
