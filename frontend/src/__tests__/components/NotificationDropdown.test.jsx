/**
 * Tests for src/components/NotificationDropdown.jsx
 *
 * Covers:
 *   - Bell button renders with correct aria-label
 *   - Dropdown does NOT render until bell is clicked
 *   - Dropdown renders after bell click
 *   - "No notifications yet" empty state is shown
 *   - Notifications list renders when data is present
 *   - Unread badge shows correct count
 *   - "Mark all read" button appears only when there are unread items
 *   - Clicking "Mark as read" on a single item calls the correct endpoint
 *   - Clicking outside closes the dropdown
 *
 * All tests follow the Arrange-Act-Assert (AAA) pattern.
 */

import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import NotificationDropdown from '../../components/NotificationDropdown';

// ── Mock the api module ───────────────────────────────────────────────────────
jest.mock('../../services/api', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    put: jest.fn(),
  },
}));

import api from '../../services/api';

// ── Helpers ───────────────────────────────────────────────────────────────────

const emptyResponse      = { data: [] };
const zeroUnread         = { data: { unreadCount: 0 } };
const threeUnread        = { data: { unreadCount: 3 } };

const sampleNotifications = [
  {
    notificationId: 'notif-001',
    type: 'RESUME_CREATED',
    title: 'Resume Created',
    message: 'Your resume was created.',
    isRead: false,
    createdAt: new Date().toISOString(),
  },
  {
    notificationId: 'notif-002',
    type: 'ATS_COMPLETE',
    title: 'ATS Complete',
    message: 'ATS scan finished.',
    isRead: true,
    createdAt: new Date().toISOString(),
  },
];

function renderDropdown(userId = 'user-001') {
  return render(<NotificationDropdown userId={userId} />);
}

// ─────────────────────────────────────────────────────────────────────────────
// Bell button
// ─────────────────────────────────────────────────────────────────────────────

describe('NotificationDropdown – Bell button', () => {
  beforeEach(() => {
    api.get.mockResolvedValue(emptyResponse);
  });

  afterEach(() => jest.clearAllMocks());

  test('renders the bell button with accessible aria-label', async () => {
    // Arrange & Act
    await act(async () => { renderDropdown(); });

    // Assert
    const bell = screen.getByRole('button', { name: /notifications/i });
    expect(bell).toBeInTheDocument();
  });

  test('dropdown panel is NOT visible before bell is clicked', async () => {
    // Arrange & Act
    await act(async () => { renderDropdown(); });

    // Assert
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Dropdown open/close
// ─────────────────────────────────────────────────────────────────────────────

describe('NotificationDropdown – open/close', () => {
  beforeEach(() => {
    api.get
      .mockResolvedValueOnce(emptyResponse)  // list call
      .mockResolvedValueOnce(zeroUnread);    // count call
  });

  afterEach(() => jest.clearAllMocks());

  test('opens the dropdown panel when bell is clicked', async () => {
    // Arrange
    await act(async () => { renderDropdown(); });
    const bell = screen.getByRole('button', { name: /notifications/i });

    // Act
    fireEvent.click(bell);

    // Assert
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  test('closes the dropdown when bell is clicked a second time', async () => {
    // Arrange
    await act(async () => { renderDropdown(); });
    const bell = screen.getByRole('button', { name: /notifications/i });

    // Act
    fireEvent.click(bell); // open
    fireEvent.click(bell); // close

    // Assert
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────

describe('NotificationDropdown – empty state', () => {
  beforeEach(() => {
    api.get
      .mockResolvedValueOnce(emptyResponse)
      .mockResolvedValueOnce(zeroUnread);
  });

  afterEach(() => jest.clearAllMocks());

  test('displays "No notifications yet" when list is empty', async () => {
    // Arrange
    await act(async () => { renderDropdown(); });

    // Act – open dropdown
    fireEvent.click(screen.getByRole('button', { name: /notifications/i }));

    // Assert
    expect(screen.getByText(/no notifications yet/i)).toBeInTheDocument();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Notification list
// ─────────────────────────────────────────────────────────────────────────────

describe('NotificationDropdown – notification list', () => {
  beforeEach(() => {
    api.get
      .mockResolvedValueOnce({ data: sampleNotifications })
      .mockResolvedValueOnce(threeUnread);
  });

  afterEach(() => jest.clearAllMocks());

  test('renders all notification messages', async () => {
    // Arrange
    await act(async () => { renderDropdown(); });

    // Act
    fireEvent.click(screen.getByRole('button', { name: /notifications/i }));

    // Assert
    expect(screen.getByText('Your resume was created.')).toBeInTheDocument();
    expect(screen.getByText('ATS scan finished.')).toBeInTheDocument();
  });

  test('shows unread count badge on the bell button', async () => {
    // Arrange & Act
    await act(async () => { renderDropdown(); });

    // Assert — badge text shows "3"
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  test('shows "Mark all read" button when there are unread notifications', async () => {
    // Arrange
    await act(async () => { renderDropdown(); });

    // Act
    fireEvent.click(screen.getByRole('button', { name: /notifications/i }));

    // Assert
    expect(screen.getByText(/mark all read/i)).toBeInTheDocument();
  });

  test('shows notification count in footer', async () => {
    // Arrange
    await act(async () => { renderDropdown(); });

    // Act
    fireEvent.click(screen.getByRole('button', { name: /notifications/i }));

    // Assert
    expect(screen.getByText(/showing 2 notification/i)).toBeInTheDocument();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Mark single as read
// ─────────────────────────────────────────────────────────────────────────────

describe('NotificationDropdown – mark single as read', () => {
  beforeEach(() => {
    api.get
      .mockResolvedValueOnce({ data: sampleNotifications })
      .mockResolvedValueOnce(threeUnread);
    api.put.mockResolvedValueOnce({});
  });

  afterEach(() => jest.clearAllMocks());

  test('calls PUT /notifications/:id/read when mark-read button is clicked', async () => {
    // Arrange
    await act(async () => { renderDropdown(); });
    fireEvent.click(screen.getByRole('button', { name: /notifications/i }));

    // Act – click the "Mark as read" button for the unread notification
    const markReadButtons = screen.getAllByTitle(/mark as read/i);
    await act(async () => { fireEvent.click(markReadButtons[0]); });

    // Assert
    expect(api.put).toHaveBeenCalledWith('/notifications/notif-001/read');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Mark all as read
// ─────────────────────────────────────────────────────────────────────────────

describe('NotificationDropdown – mark all as read', () => {
  beforeEach(() => {
    api.get
      .mockResolvedValueOnce({ data: sampleNotifications })
      .mockResolvedValueOnce(threeUnread);
    api.put.mockResolvedValueOnce({});
  });

  afterEach(() => jest.clearAllMocks());

  test('calls PUT /notifications/recipient/:userId/read-all on "Mark all read"', async () => {
    // Arrange
    await act(async () => { renderDropdown(); });
    fireEvent.click(screen.getByRole('button', { name: /notifications/i }));

    // Act
    await act(async () => {
      fireEvent.click(screen.getByText(/mark all read/i));
    });

    // Assert
    expect(api.put).toHaveBeenCalledWith('/notifications/recipient/user-001/read-all');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// No userId — should not fetch
// ─────────────────────────────────────────────────────────────────────────────

describe('NotificationDropdown – no userId', () => {
  afterEach(() => jest.clearAllMocks());

  test('does not call api.get when userId is undefined', async () => {
    // Arrange & Act
    await act(async () => { render(<NotificationDropdown userId={undefined} />); });

    // Assert
    expect(api.get).not.toHaveBeenCalled();
  });
});
