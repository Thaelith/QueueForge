import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../lib/api';
import type { Job, JobEventResponse } from '../types/api';
import StatusBadge from '../components/StatusBadge';
import Loading from '../components/Loading';

export default function JobDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [job, setJob] = useState<Job | null>(null);
  const [events, setEvents] = useState<JobEventResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMsg, setActionMsg] = useState('');

  const fetchData = () => {
    if (!id) return;
    setLoading(true);
    Promise.all([api.getJob(id), api.getJobEvents(id)])
      .then(([j, e]) => { setJob(j); setEvents(e); })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(); }, [id]);

  const doAction = (fn: () => Promise<{ success: boolean; message: string }>) => {
    setActionMsg('');
    fn()
      .then((res) => {
        setActionMsg(res.message ?? (res.success ? 'Done' : 'Failed'));
        if (res.success) fetchData();
      })
      .catch((e) => setActionMsg(e.message));
  };

  if (loading) return <Loading />;
  if (error) return <div className="text-error text-sm p-4 bg-error/10 rounded border border-error/20">{error}</div>;
  if (!job) return <div className="text-sm text-on-surface-variant">Job not found.</div>;

  return (
    <div>
      <button onClick={() => navigate('/jobs')} className="text-xs text-primary hover:underline mb-4 inline-block">&larr; Back to Jobs</button>
      <h1 className="text-h1 font-semibold text-on-surface mb-6">Job Detail</h1>

      {actionMsg && (
        <div className="mb-4 px-4 py-2 rounded text-sm border bg-surface-container border-outline-variant text-on-surface">
          {actionMsg}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
        <div className="md:col-span-2 bg-surface-container rounded-md border border-outline-variant p-4">
          <h3 className="text-sm font-semibold text-on-surface mb-3">Metadata</h3>
          <div className="grid grid-cols-2 gap-2 text-xs">
            <div className="text-on-surface-variant">ID</div><div className="font-mono text-on-surface truncate">{job.id}</div>
            <div className="text-on-surface-variant">Queue</div><div className="font-mono text-on-surface">{job.queue}</div>
            <div className="text-on-surface-variant">Type</div><div className="font-mono text-on-surface">{job.type}</div>
            <div className="text-on-surface-variant">Status</div><div><StatusBadge status={job.status} /></div>
            <div className="text-on-surface-variant">Priority</div><div className="font-mono text-on-surface">{job.priority}</div>
            <div className="text-on-surface-variant">Attempts</div><div className="font-mono text-on-surface">{job.attempts}/{job.maxAttempts}</div>
            <div className="text-on-surface-variant">Created</div><div className="text-on-surface-variant">{new Date(job.createdAt).toLocaleString()}</div>
            <div className="text-on-surface-variant">Updated</div><div className="text-on-surface-variant">{new Date(job.updatedAt).toLocaleString()}</div>
            {job.scheduledAt && <><div className="text-on-surface-variant">Scheduled</div><div className="text-on-surface-variant">{new Date(job.scheduledAt).toLocaleString()}</div></>}
          </div>
        </div>

        <div className="bg-surface-container rounded-md border border-outline-variant p-4">
          <h3 className="text-sm font-semibold text-on-surface mb-3">Actions</h3>
          <div className="space-y-2">
            {(job.status === 'DEAD_LETTERED' || job.status === 'CANCELLED') && (
              <button onClick={() => doAction(() => api.requeueJob(job.id, { resetAttempts: true }))}
                className="w-full px-3 py-1.5 text-xs rounded bg-primary/10 border border-primary/20 text-primary hover:bg-primary/20 transition-colors">
                Requeue
              </button>
            )}
            {(job.status === 'PENDING' || job.status === 'RETRY_SCHEDULED' || job.status === 'RUNNING') && (
              <button onClick={() => doAction(() => api.cancelJob(job.id))}
                className="w-full px-3 py-1.5 text-xs rounded bg-error/10 border border-error/20 text-error hover:bg-error/20 transition-colors">
                Cancel
              </button>
            )}
            {(job.status === 'RETRY_SCHEDULED' || job.status === 'DEAD_LETTERED') && (
              <button onClick={() => doAction(() => api.retryNow(job.id, { resetAttempts: true }))}
                className="w-full px-3 py-1.5 text-xs rounded bg-tertiary/10 border border-tertiary/20 text-tertiary hover:bg-tertiary/20 transition-colors">
                Retry Now
              </button>
            )}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-surface-container rounded-md border border-outline-variant p-4">
          <h3 className="text-sm font-semibold text-on-surface mb-3">Payload</h3>
          <pre className="text-xs font-mono text-on-surface bg-surface-dim rounded p-3 overflow-auto max-h-60">
            {JSON.stringify(job.payload, null, 2)}
          </pre>
        </div>

        <div className="bg-surface-container rounded-md border border-outline-variant p-4">
          <h3 className="text-sm font-semibold text-on-surface mb-3">Event Timeline</h3>
          {events.length === 0 ? (
            <p className="text-xs text-on-surface-variant">No events recorded.</p>
          ) : (
            <div className="space-y-0">
              {events.map((evt) => (
                <div key={evt.id} className="flex gap-3 py-2 border-b border-outline-variant/50 last:border-0">
                  <div className="text-[10px] text-on-surface-variant w-24 shrink-0">{new Date(evt.createdAt).toLocaleString()}</div>
                  <div>
                    <div className="text-xs font-medium text-on-surface">{evt.eventType}</div>
                    <div className="text-[11px] text-on-surface-variant">{evt.message}</div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
