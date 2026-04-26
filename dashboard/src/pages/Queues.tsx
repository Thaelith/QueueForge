import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import type { QueueStat } from '../types/api';
import Loading from '../components/Loading';
import Empty from '../components/Empty';

export default function Queues() {
  const [queues, setQueues] = useState<QueueStat[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api.getQueueStats()
      .then((res) => setQueues(res.queues))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Loading />;
  if (error) return <div className="text-error text-sm p-4 bg-error/10 rounded border border-error/20">{error}</div>;
  if (queues.length === 0) return <Empty message="No queues with data." />;

  return (
    <div>
      <h1 className="text-h1 font-semibold text-on-surface mb-4">Queues</h1>
      <div className="bg-surface-container rounded-md border border-outline-variant overflow-hidden">
        <table className="w-full text-xs">
          <thead>
            <tr className="text-on-surface-variant border-b border-outline-variant">
              <th className="text-left py-2.5 px-3 font-medium">Queue</th>
              <th className="text-right py-2.5 px-3 font-medium">Total</th>
              <th className="text-right py-2.5 px-3 font-medium">Pending</th>
              <th className="text-right py-2.5 px-3 font-medium">Running</th>
              <th className="text-right py-2.5 px-3 font-medium">Retry</th>
              <th className="text-right py-2.5 px-3 font-medium">Completed</th>
              <th className="text-right py-2.5 px-3 font-medium">DLQ</th>
              <th className="text-right py-2.5 px-3 font-medium">Cancelled</th>
              <th className="text-right py-2.5 px-3 font-medium">Oldest Pending</th>
            </tr>
          </thead>
          <tbody>
            {queues.map((q) => (
              <tr key={q.queueName} className="border-b border-outline-variant/50">
                <td className="py-2.5 px-3 font-mono text-on-surface font-medium">{q.queueName}</td>
                <td className="py-2.5 px-3 text-right font-mono">{q.total}</td>
                <td className="py-2.5 px-3 text-right font-mono text-primary">{q.pending}</td>
                <td className="py-2.5 px-3 text-right font-mono text-blue-400">{q.running}</td>
                <td className="py-2.5 px-3 text-right font-mono text-tertiary">{q.retryScheduled}</td>
                <td className="py-2.5 px-3 text-right font-mono text-green-400">{q.completed}</td>
                <td className="py-2.5 px-3 text-right font-mono text-error">{q.deadLettered}</td>
                <td className="py-2.5 px-3 text-right font-mono text-gray-400">{q.cancelled}</td>
                <td className="py-2.5 px-3 text-right text-on-surface-variant text-[11px]">
                  {q.oldestPendingAt ? new Date(q.oldestPendingAt).toLocaleString() : '–'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
