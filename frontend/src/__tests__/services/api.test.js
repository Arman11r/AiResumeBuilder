/**
 * Tests for src/services/api.js
 *
 * Verifies:
 *   - axios.create is called with baseURL: 'http://localhost:8080'
 *   - request interceptor injects Authorization header when token is in localStorage
 *   - request interceptor injects X-User-Id header when userId is in localStorage
 *   - neither header is added when localStorage is empty
 *   - config object is returned unchanged (pass-through)
 *
 * All tests follow the Arrange-Act-Assert (AAA) pattern.
 *
 * Note on jest.mock hoisting:
 *   Babel hoists jest.mock() calls above ALL import/let/const declarations, so
 *   any variable defined with let/const cannot be referenced inside the factory.
 *   The solution is to build the entire mock inside the factory and retrieve
 *   captured values later via `mock.results` / `mock.calls`.
 */

// ── Declare mock inline – everything built inside the factory ─────────────────
jest.mock('axios', () => {
  // Build the mock interceptors object inside the factory (no TDZ issue)
  const mockUse = jest.fn();
  const instance = {
    interceptors: {
      request:  { use: mockUse },
      response: { use: jest.fn() },
    },
  };
  const mockCreate = jest.fn(() => instance);

  return {
    __esModule: true,
    default: { create: mockCreate },
    // Also expose on the module root for CJS interop
    create: mockCreate,
  };
});

// ── Import AFTER mock (Babel ensures mock runs first) ─────────────────────────
import axios from 'axios';
import api from '../../services/api';   // ← triggers axios.default.create() + interceptor.use()

// ── Helpers to retrieve what api.js registered ────────────────────────────────

/** The axios instance returned by axios.create() */
function getMockInstance() {
  // ESM 'import axios from "axios"' resolves the default export directly,
  // so axios === { create: mockCreate } — NOT axios.default
  return axios.create.mock.results[0].value;
}

/** The request interceptor fn that api.js passed to interceptors.request.use() */
function getInterceptorFn() {
  return getMockInstance().interceptors.request.use.mock.calls[0]?.[0];
}

// ─────────────────────────────────────────────────────────────────────────────
// axios.create configuration
// ─────────────────────────────────────────────────────────────────────────────

describe('api.js – axios instance creation', () => {
  test('should call axios.create with baseURL: http://localhost:8080', () => {
    // Arrange – api.js ran on import, create() was already called

    // Act – (side-effect already happened)

    // Assert
    expect(axios.create).toHaveBeenCalledWith(
      expect.objectContaining({ baseURL: 'http://localhost:8080' })
    );
  });

  test('should register exactly one request interceptor', () => {
    // Arrange
    const instance = getMockInstance();

    // Assert
    expect(instance.interceptors.request.use).toHaveBeenCalledTimes(1);
    expect(getInterceptorFn()).toBeInstanceOf(Function);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Request interceptor behaviour
// ─────────────────────────────────────────────────────────────────────────────

describe('api.js – request interceptor', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  test('should inject Bearer token when token is in localStorage', () => {
    // Arrange
    localStorage.setItem('token', 'my-jwt-token');
    const interceptorFn = getInterceptorFn();

    // Act
    const config = interceptorFn({ headers: {} });

    // Assert
    expect(config.headers.Authorization).toBe('Bearer my-jwt-token');
  });

  test('should NOT inject Authorization header when no token in localStorage', () => {
    // Arrange – localStorage cleared in beforeEach
    const interceptorFn = getInterceptorFn();

    // Act
    const config = interceptorFn({ headers: {} });

    // Assert
    expect(config.headers.Authorization).toBeUndefined();
  });

  test('should inject X-User-Id header when userId is in localStorage', () => {
    // Arrange
    localStorage.setItem('userId', 'user-uuid-001');
    const interceptorFn = getInterceptorFn();

    // Act
    const config = interceptorFn({ headers: {} });

    // Assert
    expect(config.headers['X-User-Id']).toBe('user-uuid-001');
  });

  test('should NOT inject X-User-Id when userId is absent from localStorage', () => {
    // Arrange – localStorage cleared in beforeEach
    const interceptorFn = getInterceptorFn();

    // Act
    const config = interceptorFn({ headers: {} });

    // Assert
    expect(config.headers['X-User-Id']).toBeUndefined();
  });

  test('should return the same config object (pass-through)', () => {
    // Arrange
    const inputConfig = { headers: { 'Content-Type': 'application/json' } };
    const interceptorFn = getInterceptorFn();

    // Act
    const result = interceptorFn(inputConfig);

    // Assert
    expect(result).toBe(inputConfig);
  });
});
