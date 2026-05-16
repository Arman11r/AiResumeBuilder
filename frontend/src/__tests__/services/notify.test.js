/**
 * Tests for src/services/notify.js
 *
 * Verifies:
 *   - pushNotification() calls api.post with the correct endpoint and payload
 *   - pushNotification() silently ignores API errors (never throws)
 *   - relatedId defaults to null when not provided
 *
 * All tests follow the Arrange-Act-Assert (AAA) pattern.
 */

// ── Mock the api module ───────────────────────────────────────────────────────
jest.mock('../../services/api', () => ({
  __esModule: true,
  default: {
    post: jest.fn(),
  },
}));

import api from '../../services/api';
import { pushNotification } from '../../services/notify';

// ─────────────────────────────────────────────────────────────────────────────
// pushNotification()
// ─────────────────────────────────────────────────────────────────────────────

describe('notify.js – pushNotification()', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('should call api.post with /notifications and the correct payload', async () => {
    // Arrange
    api.post.mockResolvedValueOnce({ data: {} });

    const params = {
      recipientId: 'user-001',
      type: 'RESUME_CREATED',
      message: 'Your resume was created.',
      relatedId: 'resume-abc',
    };

    // Act
    pushNotification(params);

    // Assert
    expect(api.post).toHaveBeenCalledWith('/notifications', {
      recipientId: 'user-001',
      type: 'RESUME_CREATED',
      message: 'Your resume was created.',
      relatedId: 'resume-abc',
    });
  });

  test('should default relatedId to null when it is not provided', async () => {
    // Arrange
    api.post.mockResolvedValueOnce({ data: {} });

    // Act
    pushNotification({
      recipientId: 'user-001',
      type: 'ATS_COMPLETE',
      message: 'ATS scan done.',
      // relatedId intentionally omitted
    });

    // Assert
    expect(api.post).toHaveBeenCalledWith('/notifications', {
      recipientId: 'user-001',
      type: 'ATS_COMPLETE',
      message: 'ATS scan done.',
      relatedId: null,
    });
  });

  test('should NOT throw when api.post rejects (fire-and-forget)', async () => {
    // Arrange
    api.post.mockRejectedValueOnce(new Error('Network error'));

    // Act & Assert – must not throw
    expect(() =>
      pushNotification({
        recipientId: 'user-001',
        type: 'EXPORT_READY',
        message: 'Your export is ready.',
      })
    ).not.toThrow();
  });

  test('should call api.post exactly once per invocation', () => {
    // Arrange
    api.post.mockResolvedValueOnce({ data: {} });

    // Act
    pushNotification({ recipientId: 'u-1', type: 'USER_LOGIN', message: 'Welcome!' });

    // Assert
    expect(api.post).toHaveBeenCalledTimes(1);
  });
});
