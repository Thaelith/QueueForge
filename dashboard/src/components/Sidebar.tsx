import { NavLink } from 'react-router-dom';

const links = [
  { to: '/', label: 'Overview', icon: '◉' },
  { to: '/jobs', label: 'Jobs', icon: '☰' },
  { to: '/queues', label: 'Queues', icon: '⧉' },
  { to: '/workers', label: 'Workers', icon: '⛭' },
  { to: '/dead-letter', label: 'Dead Letter', icon: '⚠' },
  { to: '/settings', label: 'Settings', icon: '⚙' },
];

export default function Sidebar() {
  return (
    <aside className="w-56 bg-surface-container-low border-r border-outline-variant flex flex-col shrink-0">
      <div className="px-5 py-4 border-b border-outline-variant">
        <div className="text-sm font-semibold tracking-tight text-on-surface">QueueForge</div>
        <div className="text-[11px] text-on-surface-variant mt-0.5 font-mono">Admin Dashboard</div>
      </div>
      <nav className="flex-1 px-3 py-3 space-y-0.5">
        {links.map(({ to, label, icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded text-sm transition-colors ${
                isActive
                  ? 'bg-surface-container-high text-primary font-medium'
                  : 'text-on-surface-variant hover:bg-surface-container hover:text-on-surface'
              }`
            }
          >
            <span className="text-xs opacity-60 w-4 text-center font-mono">{icon}</span>
            {label}
          </NavLink>
        ))}
      </nav>
      <div className="px-5 py-3 border-t border-outline-variant text-[11px] text-on-surface-variant font-mono">
        QueueForge v1.0
      </div>
    </aside>
  );
}
