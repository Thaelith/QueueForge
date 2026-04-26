import { useEffect, useState } from 'react';

export default function Settings() {
  const [health, setHealth] = useState<string>('checking...');
  const baseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

  useEffect(() => {
    fetch(`${baseUrl}/actuator/health`)
      .then((r) => r.json())
      .then((d) => setHealth(d.status ?? JSON.stringify(d)))
      .catch(() => setHealth('unreachable'));
  }, [baseUrl]);

  return (
    <div>
      <h1 className="text-h1 font-semibold text-on-surface mb-6">Settings</h1>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-surface-container rounded-md border border-outline-variant p-4">
          <h3 className="text-sm font-semibold text-on-surface mb-3">Connection</h3>
          <div className="space-y-2 text-xs">
            <div className="flex justify-between">
              <span className="text-on-surface-variant">API Base URL</span>
              <span className="font-mono text-on-surface">{baseUrl}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-on-surface-variant">Backend Health</span>
              <span className={`font-mono font-medium ${health === 'UP' ? 'text-green-400' : health === 'unreachable' ? 'text-error' : 'text-tertiary'}`}>
                {health}
              </span>
            </div>
          </div>
        </div>

        <div className="bg-surface-container rounded-md border border-outline-variant p-4">
          <h3 className="text-sm font-semibold text-on-surface mb-3">Links</h3>
          <div className="space-y-2">
            <a href={`${baseUrl}/swagger-ui.html`} target="_blank" rel="noopener noreferrer"
              className="block text-xs text-primary hover:underline">Swagger UI &rarr;</a>
            <a href={`${baseUrl}/actuator/prometheus`} target="_blank" rel="noopener noreferrer"
              className="block text-xs text-primary hover:underline">Prometheus Metrics &rarr;</a>
            <a href="http://localhost:3000" target="_blank" rel="noopener noreferrer"
              className="block text-xs text-primary hover:underline">Grafana (localhost:3000) &rarr;</a>
          </div>
        </div>

        <div className="bg-surface-container rounded-md border border-outline-variant p-4 md:col-span-2">
          <h3 className="text-sm font-semibold text-on-surface mb-3">Build Info</h3>
          <div className="space-y-1 text-xs">
            <div className="flex justify-between">
              <span className="text-on-surface-variant">Dashboard Version</span>
              <span className="font-mono text-on-surface">1.0.0</span>
            </div>
            <div className="flex justify-between">
              <span className="text-on-surface-variant">Stack</span>
              <span className="font-mono text-on-surface">React + TypeScript + Vite + Tailwind CSS</span>
            </div>
            <div className="flex justify-between">
              <span className="text-on-surface-variant">Backend</span>
              <span className="font-mono text-on-surface">Java 21 + Spring Boot 3.3 + PostgreSQL 16</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
