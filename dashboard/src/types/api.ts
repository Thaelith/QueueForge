export type JobStatus = 'PENDING' | 'RUNNING' | 'RETRY_SCHEDULED' | 'COMPLETED' | 'DEAD_LETTERED' | 'CANCELLED';

export type JobPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'CRITICAL';

export interface Job {
  id: string;
  queue: string;
  type: string;
  payload: Record<string, unknown>;
  status: JobStatus;
  priority: number;
  attempts: number;
  maxAttempts: number;
  scheduledAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardSummaryResponse {
  totalJobs: number;
  pendingJobs: number;
  runningJobs: number;
  completedJobs: number;
  retryScheduledJobs: number;
  deadLetteredJobs: number;
  cancelledJobs: number;
  activeWorkers: number;
  completedLastHour: number;
  failedLastHour: number;
  jobsByQueue: Record<string, number>;
}

export interface QueueStat {
  queueName: string;
  pending: number;
  running: number;
  retryScheduled: number;
  completed: number;
  deadLettered: number;
  cancelled: number;
  total: number;
  oldestPendingAt: string | null;
  newestCreatedAt: string | null;
}

export interface QueueStatsResponse {
  queues: QueueStat[];
}

export interface WorkerInfo {
  workerId: string;
  status: string;
  runningJobCount: number;
  completedJobCount: number;
  failedJobCount: number;
  lastSeenAt: string | null;
}

export interface WorkerSummaryResponse {
  workers: WorkerInfo[];
}

export interface JobEventResponse {
  id: number;
  eventType: string;
  createdAt: string;
  message: string;
}

export interface AdminActionResponse {
  success: boolean;
  message: string;
  jobId: string;
  newStatus: string;
}

export interface WorkerConfig {
  workerEnabled: boolean;
  workerId: string;
  queues: string[];
  pollIntervalMs: number;
  leaseDurationSeconds: number;
  maxJobsPerPoll: number;
  recoveryEnabled: boolean;
  recoveryIntervalMs: number;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}
