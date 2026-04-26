import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import type { DashboardSummaryResponse, QueueStat, WorkerInfo } from '../types/api';
import MetricCard from '../components/MetricCard';
import Loading from '../components/Loading';

export default function Overview() {
  const [summary, setSummary] = useState<DashboardSummaryResponse | null>(null);
  const [queues, setQueues] = useState<QueueStat[]>([]);
  const [workers, setWorkers] = useState<WorkerInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([
      api.getDashboardSummary(),
      api.getQueueStats(),
      api.getWorkers(),
    ])
      .then(([s, q, w]) => {
        setSummary(s);
        setQueues(q.queues);
        setWorkers(w.workers);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Loading />;
  if (error) return <div className="text-error text-sm p-4 bg-error/10 rounded border border-error/20">{error}</div>;
  if (!summary) return null;

  return (
    <div>
      <h1 className="text-h1 font-semibold text-on-surface mb-6">Overview</h1>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <MetricCard label="Total Jobs" value={summary.totalJobs} />
        <MetricCard label="Pending" value={summary.pendingJobs} accent="blue" />
        <MetricCard label="Running" value={summary.runningJobs} accent="blue" />
        <MetricCard label="Completed" value={summary.completedJobs} accent="green" />
        <MetricCard label="Retry Scheduled" value={summary.retryScheduledJobs} accent="amber" />
        <MetricCard label="Dead Lettered" value={summary.deadLetteredJobs} accent="red" />
        <MetricCard label="Cancelled" value={summary.cancelledJobs} />
        <MetricCard label="Last Hour Completed" value={summary.completedLastHour} accent="green" sub="past 60 min" />
        <MetricCard label="Last Hour Failed" value={summary.failedLastHour} accent="red" sub="past 60 min" />
        <MetricCard label="Active Workers" value={summary.activeWorkers} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-surface-container rounded-md border border-outline-variant p-4">
          <h3 className="text-sm font-semibold text-on-surface mb-3">Queues</h3>
          {queues.length === 0 ? (
            <p className="text-xs text-on-surface-variant">No queues with jobs.</p>
          ) : (
            <table className="w-full text-xs">
              <thead>
                <tr className="text-on-surface-variant border-b border-outline-variant">
                  <th className="text-left py-2 font-medium">Queue</th>
                  <th className="text-right py-2 font-medium">Total</th>
                  <th className="text-right py-2 font-medium">Pending</th>
                  <th className="text-right py-2 font-medium">DLQ</th>
                </tr>
              </thead>
              <tbody>
                {queues.map((q) => (
                  <tr key={q.queueName} className="border-b border-outline-variant/50">
                    <td className="py-2 font-mono text-on-surface">{q.queueName}</td>
                    <td className="py-2 text-right font-mono">{q.total}</td>
                    <td className="py-2 text-right font-mono text-primary">{q.pending}</td>
                    <td className="py-2 text-right font-mono text-error">{q.deadLettered}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="bg-surface-container rounded-md border border-outline-variant p-4">
          <h3 className="text-sm font-semibold text-on-surface mb-3">Workers</h3>
          {workers.length === 0 ? (
            <p className="text-xs text-on-surface-variant">No workers seen.</p>
          ) : (
            <div className="space-y-2">
              {workers.slice(0, 8).map((w) => (
                <div key={w.workerId} className="flex items-center justify-between bg-surface-container-high rounded px-3 py-2">
                  <div>
                    <div className="text-sm font-mono text-on-surface">{w.workerId}</div>
                    <div className="text-[11px] text-on-surface-variant">
                      {w.completedJobCount} completed &middot; {w.failedJobCount} failed &middot; {w.runningJobCount} running
                    </div>
                  </div>
                  <span className={`text-[11px] font-medium px-2 py-0.5 rounded ${
                    w.status === 'ACTIVE' ? 'bg-green-500/10 text-green-400' :
                    w.status === 'STALE' ? 'bg-error/10 text-error' :
                    'bg-gray-500/10 text-gray-400'
                  }`}>
                    {w.status}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
