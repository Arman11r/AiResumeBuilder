/**
 * Fire-and-forget helper to post a notification to the backend.
 * Never throws — safe to call without try/catch.
 */
import api from './api';

/**
 * @param {object} params
 * @param {string} params.recipientId   - userId of the recipient
 * @param {string} params.type          - notification type enum (RESUME_CREATED, ATS_COMPLETE, etc.)
 * @param {string} params.message       - human-readable body text
 * @param {string} [params.relatedId]   - optional related entity ID (resumeId, matchId …)
 */
export function pushNotification({ recipientId, type, message, relatedId }) {
  api.post('/notifications', {
    recipientId,
    type,
    message,
    relatedId: relatedId || null,
  }).catch(() => {/* silently ignore backend errors */});
}
