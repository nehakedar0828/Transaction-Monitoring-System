import React from 'react';
import { Shield, RefreshCw, Sun, Moon } from 'lucide-react';

export default function Header({
  onRefresh,
  isRefreshing,
  theme,
  onToggleTheme,
}) {
  return (
    <header className="header">
      <div className="brand">
        <div className="brand-icon">
          <Shield size={20} strokeWidth={2.5} />
        </div>
        <div>
          <div className="brand-title">FraudShield Transaction Monitor</div>
          <div className="brand-subtitle">
            Enterprise Transaction Surveillance &amp; Scikit-Learn Inference Pipeline
          </div>
        </div>
      </div>

      <div className="header-actions">
        {/* Theme Toggle Button */}
        <button
          className="btn btn-secondary"
          onClick={onToggleTheme}
          aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
          title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
        >
          {theme === 'dark' ? (
            <>
              <Sun size={15} />
              <span>Light Mode</span>
            </>
          ) : (
            <>
              <Moon size={15} />
              <span>Dark Mode</span>
            </>
          )}
        </button>

        {/* Live Refresh Button */}
        <button
          className="btn btn-primary"
          onClick={onRefresh}
          disabled={isRefreshing}
          aria-label="Refresh dashboard data"
          title="Query latest dataset metrics and transactions from backend"
        >
          <RefreshCw size={14} className={isRefreshing ? 'spinner' : ''} />
          <span>{isRefreshing ? 'Refreshing...' : 'Refresh'}</span>
        </button>
      </div>
    </header>
  );
}
