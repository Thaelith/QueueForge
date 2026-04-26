import type { JobStatus } from '../types/api';

const statusColors: Record<JobStatus, string> = {
  PENDING: 'bg-primary/10 text-primary border-primary/20',
  RUNNING: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  COMPLETED: 'bg-green-500/10 text-green-400 border-green-500/20',
  RETRY_SCHEDULED: 'bg-tertiary/10 text-tertiary border-tertiary/20',
  DEAD_LETTERED: 'bg-error/10 text-error border-error/20',
  CANCELLED: 'bg-gray-500/10 text-gray-400 border-gray-500/20',
};

export default function StatusBadge({ status }: { status: JobStatus }) {
  return (
    <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-xs font-medium border ${statusColors[status]}`}>
      <span className="w-1.5 h-1.5 rounded-full bg-current" />
      {status.replace('_', ' ')}
    </span>
  );
}
