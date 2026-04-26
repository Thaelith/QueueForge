import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api';
import type { Job } from '../types/api';
import Loading from '../components/Loading';
import Empty from '../components/Empty';

export default function DeadLetter() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const fetchJobs = () => {
    setLoading(true);
    api.getJobs({ status: 'DEAD_LETTERED', size: '50', sort: 'updatedAt,desc' })
      .then((res) => setJobs(res.content))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchJobs(); }, []);

  if (loading) return <Loading />;
  if (error) return <div className="text-error text-sm p-4 bg-error/10 rounded border border-error/20">{error}</div>;
  if (jobs.length === 0) return <Empty message="No dead-lettered jobs." />;

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-h1 font-semibold text-on-surface">Dead Letter Queue</h1>
        <button onClick={fetchJobs} className="px-3 py-1.5 text-xs rounded bg-surface-container-high hover:bg-surface-container-highest border border-outline-variant text-on-surface transition-colors">
          Refresh
        </button>
      </div>
      <div className="bg-surface-container rounded-md border border-outline-variant overflow-hidden">
        <table className="w-full text-xs">
          <thead>
            <tr className="text-on-surface-variant border-b border-outline-variant">
              <th className="text-left py-2.5 px-3 font-medium">ID</th>
              <th className="text-left py-2.5 px-3 font-medium">Queue</th>
              <th className="text-left py-2.5 px-3 font-medium">Type</th>
              <th className="text-right py-2.5 px-3 font-medium">Attempts</th>
              <th className="text-right py-2.5 px-3 font-medium">Updated</th>
              <th className="text-right py-2.5 px-3 font-medium">Actions</th>
            </tr>
          </thead>
          <tbody>
            {jobs.map((job) => (
              <tr key={job.id} className="border-b border-outline-variant/50 hover:bg-surface-container-high cursor-pointer transition-colors"
                onClick={() => navigate(`/jobs/${job.id}`)}>
                <td className="py-2.5 px-3 font-mono text-on-surface truncate max-w-[140px]">{job.id}</td>
                <td className="py-2.5 px-3 font-mono text-on-surface-variant">{job.queue}</td>
                <td className="py-2.5 px-3 text-on-surface text-[11px]">{job.type}</td>
                <td className="py-2.5 px-3 text-right font-mono">{job.attempts}/{job.maxAttempts}</td>
                <td className="py-2.5 px-3 text-right text-on-surface-variant">{new Date(job.updatedAt).toLocaleString()}</td>
                <td className="py-2.5 px-3 text-right" onClick={(e) => e.stopPropagation()}>
                  <button onClick={() => api.requeueJob(job.id, { resetAttempts: true }).then(() => fetchJobs())}
                    className="px-2 py-1 text-[11px] rounded bg-primary/10 border border-primary/20 text-primary hover:bg-primary/20 transition-colors">
                    Requeue
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
