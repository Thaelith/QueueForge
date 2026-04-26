import { useEffect, useState, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { api } from '../lib/api';
import type { Job } from '../types/api';
import StatusBadge from '../components/StatusBadge';
import Loading from '../components/Loading';
import Empty from '../components/Empty';

export default function JobsList() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  const page = parseInt(searchParams.get('page') ?? '0');
  const status = searchParams.get('status') ?? '';
  const queue = searchParams.get('queue') ?? '';

  const fetchJobs = useCallback(() => {
    setLoading(true);
    setError('');
    const params: Record<string, string> = { page: String(page), size: '20', sort: 'createdAt,desc' };
    if (status) params.status = status;
    if (queue) params.queue = queue;

    api.getJobs(params)
      .then((res) => {
        setJobs(res.content);
        setTotalElements(res.totalElements);
        setTotalPages(res.totalPages);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [page, status, queue]);

  useEffect(() => { fetchJobs(); }, [fetchJobs]);

  const setParam = (key: string, value: string) => {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(key, value); else next.delete(key);
    next.delete('page');
    setSearchParams(next);
  };

  return (
    <div>
      <h1 className="text-h1 font-semibold text-on-surface mb-4">Jobs</h1>

      <div className="flex gap-3 mb-4">
        <select value={status} onChange={(e) => setParam('status', e.target.value)}
          className="bg-surface-container border border-outline-variant rounded px-3 py-1.5 text-sm text-on-surface">
          <option value="">All Statuses</option>
          <option value="PENDING">Pending</option>
          <option value="RUNNING">Running</option>
          <option value="RETRY_SCHEDULED">Retry Scheduled</option>
          <option value="COMPLETED">Completed</option>
          <option value="DEAD_LETTERED">Dead Lettered</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
        <input value={queue} onChange={(e) => setParam('queue', e.target.value)}
          placeholder="Filter queue..."
          className="bg-surface-container border border-outline-variant rounded px-3 py-1.5 text-sm text-on-surface w-48" />
        <button onClick={() => fetchJobs()} className="px-4 py-1.5 rounded text-sm bg-surface-container-high hover:bg-surface-container-highest border border-outline-variant text-on-surface transition-colors">
          Refresh
        </button>
      </div>

      {loading ? <Loading /> :
       error ? <div className="text-error text-sm p-4 bg-error/10 rounded border border-error/20">{error}</div> :
       jobs.length === 0 ? <Empty message="No jobs found." /> : (
        <>
          <div className="bg-surface-container rounded-md border border-outline-variant overflow-hidden">
            <table className="w-full text-xs">
              <thead>
                <tr className="text-on-surface-variant border-b border-outline-variant">
                  <th className="text-left py-2.5 px-3 font-medium">ID</th>
                  <th className="text-left py-2.5 px-3 font-medium">Queue</th>
                  <th className="text-left py-2.5 px-3 font-medium">Type</th>
                  <th className="text-left py-2.5 px-3 font-medium">Status</th>
                  <th className="text-right py-2.5 px-3 font-medium">Priority</th>
                  <th className="text-right py-2.5 px-3 font-medium">Attempts</th>
                  <th className="text-right py-2.5 px-3 font-medium">Created</th>
                </tr>
              </thead>
              <tbody>
                {jobs.map((job) => (
                  <tr key={job.id} onClick={() => navigate(`/jobs/${job.id}`)}
                    className="border-b border-outline-variant/50 hover:bg-surface-container-high cursor-pointer transition-colors">
                    <td className="py-2.5 px-3 font-mono text-on-surface truncate max-w-[160px]">{job.id}</td>
                    <td className="py-2.5 px-3 font-mono text-on-surface-variant">{job.queue}</td>
                    <td className="py-2.5 px-3 font-mono text-on-surface text-[11px]">{job.type}</td>
                    <td className="py-2.5 px-3"><StatusBadge status={job.status} /></td>
                    <td className="py-2.5 px-3 text-right font-mono">{job.priority}</td>
                    <td className="py-2.5 px-3 text-right font-mono">{job.attempts}/{job.maxAttempts}</td>
                    <td className="py-2.5 px-3 text-right text-on-surface-variant">{new Date(job.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="flex justify-between items-center mt-3 text-xs text-on-surface-variant">
            <span>{totalElements} jobs</span>
            <div className="flex gap-2">
              <button disabled={page === 0} onClick={() => setParam('page', String(page - 1))}
                className="px-3 py-1 rounded bg-surface-container-high border border-outline-variant disabled:opacity-30 hover:bg-surface-container-highest transition-colors">
                Prev
              </button>
              <span className="px-3 py-1">{page + 1} / {Math.max(totalPages, 1)}</span>
              <button disabled={page >= totalPages - 1} onClick={() => setParam('page', String(page + 1))}
                className="px-3 py-1 rounded bg-surface-container-high border border-outline-variant disabled:opacity-30 hover:bg-surface-container-highest transition-colors">
                Next
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
