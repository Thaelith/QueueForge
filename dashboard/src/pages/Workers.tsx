import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import type { WorkerInfo, WorkerConfig } from '../types/api';
import Loading from '../components/Loading';
import Empty from '../components/Empty';

export default function Workers() {
  const [workers, setWorkers] = useState<WorkerInfo[]>([]);
  const [config, setConfig] = useState<WorkerConfig | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([api.getWorkers(), api.getWorkerConfig()])
      .then(([w, c]) => { setWorkers(w.workers); setConfig(c); })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Loading />;
  if (error) return <div className="text-error text-sm p-4 bg-error/10 rounded border border-error/20">{error}</div>;

  return (
    <div>
      <h1 className="text-h1 font-semibold text-on-surface mb-6">Workers</h1>

      {config && (
        <div className="bg-surface-container rounded-md border border-outline-variant p-4 mb-6">
          <h3 className="text-sm font-semibold text-on-surface mb-3">Configuration</h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-2 text-xs">
            <div className="text-on-surface-variant">Status</div><div className={`font-mono ${config.workerEnabled ? 'text-green-400' : 'text-on-surface-variant'}`}>{config.workerEnabled ? 'Enabled' : 'Disabled'}</div>
            <div className="text-on-surface-variant">Worker ID</div><div className="font-mono text-on-surface">{config.workerId}</div>
            <div className="text-on-surface-variant">Queues</div><div className="font-mono text-on-surface">{config.queues.join(', ')}</div>
            <div className="text-on-surface-variant">Poll Interval</div><div className="font-mono text-on-surface">{config.pollIntervalMs}ms</div>
            <div className="text-on-surface-variant">Lease Duration</div><div className="font-mono text-on-surface">{config.leaseDurationSeconds}s</div>
            <div className="text-on-surface-variant">Max Per Poll</div><div className="font-mono text-on-surface">{config.maxJobsPerPoll}</div>
          </div>
        </div>
      )}

      {workers.length === 0 ? (
        <Empty message="No workers seen. Run a worker to populate." />
      ) : (
        <div className="bg-surface-container rounded-md border border-outline-variant overflow-hidden">
          <table className="w-full text-xs">
            <thead>
              <tr className="text-on-surface-variant border-b border-outline-variant">
                <th className="text-left py-2.5 px-3 font-medium">Worker ID</th>
                <th className="text-left py-2.5 px-3 font-medium">Status</th>
                <th className="text-right py-2.5 px-3 font-medium">Running</th>
                <th className="text-right py-2.5 px-3 font-medium">Completed</th>
                <th className="text-right py-2.5 px-3 font-medium">Failed</th>
                <th className="text-right py-2.5 px-3 font-medium">Last Seen</th>
              </tr>
            </thead>
            <tbody>
              {workers.map((w) => (
                <tr key={w.workerId} className="border-b border-outline-variant/50">
                  <td className="py-2.5 px-3 font-mono text-on-surface">{w.workerId}</td>
                  <td className="py-2.5 px-3">
                    <span className={`text-[11px] font-medium px-2 py-0.5 rounded ${
                      w.status === 'ACTIVE' ? 'bg-green-500/10 text-green-400' :
                      w.status === 'STALE' ? 'bg-error/10 text-error' :
                      'bg-gray-500/10 text-gray-400'
                    }`}>
                      {w.status}
                    </span>
                  </td>
                  <td className="py-2.5 px-3 text-right font-mono text-blue-400">{w.runningJobCount}</td>
                  <td className="py-2.5 px-3 text-right font-mono text-green-400">{w.completedJobCount}</td>
                  <td className="py-2.5 px-3 text-right font-mono text-error">{w.failedJobCount}</td>
                  <td className="py-2.5 px-3 text-right text-on-surface-variant text-[11px]">{w.lastSeenAt ? new Date(w.lastSeenAt).toLocaleString() : '–'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
