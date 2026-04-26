const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`${res.status} ${res.statusText}: ${body}`);
  }
  return res.json();
}

export const api = {
  getJobs: (params?: Record<string, string>) => {
    const qs = params ? '?' + new URLSearchParams(params).toString() : '';
    return request<import('../types/api').PaginatedResponse<import('../types/api').Job>>(`/api/v1/jobs${qs}`);
  },

  getJob: (id: string) =>
    request<import('../types/api').Job>(`/api/v1/jobs/${id}`),

  submitJob: (data: Record<string, unknown>) =>
    request<import('../types/api').Job>('/api/v1/jobs', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  // Admin
  getDashboardSummary: () =>
    request<import('../types/api').DashboardSummaryResponse>('/api/v1/admin/dashboard/summary'),

  getQueueStats: () =>
    request<import('../types/api').QueueStatsResponse>('/api/v1/admin/queues/stats'),

  getWorkers: () =>
    request<import('../types/api').WorkerSummaryResponse>('/api/v1/admin/workers'),

  getJobEvents: (jobId: string) =>
    request<import('../types/api').JobEventResponse[]>(`/api/v1/admin/jobs/${jobId}/events`),

  requeueJob: (jobId: string, body?: { resetAttempts?: boolean; reason?: string }) =>
    request<import('../types/api').AdminActionResponse>(`/api/v1/admin/jobs/${jobId}/requeue`, {
      method: 'POST',
      body: JSON.stringify(body ?? {}),
    }),

  cancelJob: (jobId: string) =>
    request<import('../types/api').AdminActionResponse>(`/api/v1/admin/jobs/${jobId}/cancel`, {
      method: 'POST',
    }),

  retryNow: (jobId: string, body?: { resetAttempts?: boolean }) =>
    request<import('../types/api').AdminActionResponse>(`/api/v1/admin/jobs/${jobId}/retry-now`, {
      method: 'POST',
      body: JSON.stringify(body ?? {}),
    }),

  // Worker
  getWorkerConfig: () =>
    request<import('../types/api').WorkerConfig>('/api/v1/workers/config'),

  runOnce: (workerId: string) =>
    request<{ workerId: string; durationMs: number }>(`/api/v1/workers/${workerId}/run-once`, {
      method: 'POST',
    }),

  // Health
  health: () => request<Record<string, unknown>>('/actuator/health'),
};
