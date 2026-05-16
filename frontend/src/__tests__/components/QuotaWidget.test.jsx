/**
 * Tests for src/components/QuotaWidget.jsx
 *
 * Covers:
 *   - renders nothing when quota is null (API not yet loaded)
 *   - shows "Premium Plan: Unlimited AI Requests" for premium users
 *   - shows "Free Daily Quota" panel for free users
 *   - displays correct remaining counts
 *   - shows "0 left" when quota is exhausted
 *   - does NOT call api.get when user is not logged in
 *
 * All tests follow the Arrange-Act-Assert (AAA) pattern.
 */

import React from 'react';
import { render, screen, act } from '@testing-library/react';
import '@testing-library/jest-dom';

// ── Mock api BEFORE importing the component ───────────────────────────────────
jest.mock('../../services/api', () => ({
  __esModule: true,
  default: { get: jest.fn() },
}));

// ── Mock AuthContext: use require() inside factory so React is in scope ───────
jest.mock('../../context/AuthContext', () => {
  const React = require('react');
  return {
    AuthContext: React.createContext({ user: null }),
  };
});

import api from '../../services/api';
import { AuthContext } from '../../context/AuthContext';
import QuotaWidget from '../../components/QuotaWidget';

// ── Helper: wrap component in a context provider ──────────────────────────────
function renderWithUser(userValue) {
  return render(
    <AuthContext.Provider value={{ user: userValue }}>
      <QuotaWidget />
    </AuthContext.Provider>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// No user logged in
// ─────────────────────────────────────────────────────────────────────────────

describe('QuotaWidget – no user', () => {
  afterEach(() => jest.clearAllMocks());

  test('renders nothing and does not call api.get when user is null', async () => {
    // Arrange & Act
    const { container } = await act(async () => renderWithUser(null));

    // Assert
    expect(container).toBeEmptyDOMElement();
    expect(api.get).not.toHaveBeenCalled();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Loading state (API pending)
// ─────────────────────────────────────────────────────────────────────────────

describe('QuotaWidget – loading state', () => {
  afterEach(() => jest.clearAllMocks());

  test('renders nothing while the quota API call is in-flight', async () => {
    // Arrange – promise that never resolves = perpetual loading
    api.get.mockImplementation(() => new Promise(() => {}));

    // Act
    const { container } = render(
      <AuthContext.Provider value={{ user: { userId: 'user-001' } }}>
        <QuotaWidget />
      </AuthContext.Provider>
    );

    // Assert – component returns null until data arrives
    expect(container).toBeEmptyDOMElement();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Premium user
// ─────────────────────────────────────────────────────────────────────────────

describe('QuotaWidget – premium user', () => {
  beforeEach(() => {
    api.get.mockResolvedValueOnce({ data: { isPremium: true } });
  });
  afterEach(() => jest.clearAllMocks());

  test('shows "Premium Plan: Unlimited AI Requests" banner', async () => {
    // Arrange & Act
    await act(async () => renderWithUser({ userId: 'user-001' }));

    // Assert
    expect(screen.getByText(/Premium Plan: Unlimited AI Requests/i)).toBeInTheDocument();
  });

  test('does NOT show the "Free Daily Quota" panel for a premium user', async () => {
    // Arrange & Act
    await act(async () => renderWithUser({ userId: 'user-001' }));

    // Assert
    expect(screen.queryByText(/Free Daily Quota/i)).not.toBeInTheDocument();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Free user — quota available
// ─────────────────────────────────────────────────────────────────────────────

describe('QuotaWidget – free user with remaining quota', () => {
  beforeEach(() => {
    api.get.mockResolvedValueOnce({
      data: {
        isPremium: false,
        contentCallsUsed: 3,
        contentCallsRemaining: 7,
        atsCallsUsed: 1,
        atsCallsRemaining: 4,
      },
    });
  });
  afterEach(() => jest.clearAllMocks());

  test('shows "Free Daily Quota" heading', async () => {
    // Arrange & Act
    await act(async () => renderWithUser({ userId: 'user-001' }));

    // Assert
    expect(screen.getByText(/Free Daily Quota/i)).toBeInTheDocument();
  });

  test('displays correct content calls remaining', async () => {
    // Arrange & Act
    await act(async () => renderWithUser({ userId: 'user-001' }));

    // Assert
    expect(screen.getByText('7 left')).toBeInTheDocument();
  });

  test('displays correct ATS calls remaining', async () => {
    // Arrange & Act
    await act(async () => renderWithUser({ userId: 'user-001' }));

    // Assert
    expect(screen.getByText('4 left')).toBeInTheDocument();
  });

  test('calls api.get with the correct quota endpoint', async () => {
    // Arrange & Act
    await act(async () => renderWithUser({ userId: 'user-001' }));

    // Assert
    expect(api.get).toHaveBeenCalledWith('/ai/quota/user-001');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Free user — quota exhausted
// ─────────────────────────────────────────────────────────────────────────────

describe('QuotaWidget – free user with exhausted quota', () => {
  beforeEach(() => {
    api.get.mockResolvedValueOnce({
      data: {
        isPremium: false,
        contentCallsUsed: 10,
        contentCallsRemaining: 0,
        atsCallsUsed: 5,
        atsCallsRemaining: 0,
      },
    });
  });
  afterEach(() => jest.clearAllMocks());

  test('shows "0 left" for both content and ATS calls when quota is exhausted', async () => {
    // Arrange & Act
    await act(async () => renderWithUser({ userId: 'user-001' }));

    // Assert
    const zeroLabels = screen.getAllByText('0 left');
    expect(zeroLabels).toHaveLength(2);
  });
});
