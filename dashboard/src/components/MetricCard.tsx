interface MetricCardProps {
  label: string;
  value: number | string;
  sub?: string;
  accent?: 'default' | 'green' | 'amber' | 'red' | 'blue';
}

const accentStyles: Record<string, string> = {
  default: 'text-on-surface',
  green: 'text-green-400',
  amber: 'text-tertiary',
  red: 'text-error',
  blue: 'text-primary',
};

export default function MetricCard({ label, value, sub, accent = 'default' }: MetricCardProps) {
  return (
    <div className="bg-surface-container rounded-md border border-outline-variant p-4">
      <div className="text-xs text-on-surface-variant font-medium uppercase tracking-wide mb-1">{label}</div>
      <div className={`text-2xl font-semibold font-mono tracking-tight ${accentStyles[accent]}`}>{value}</div>
      {sub && <div className="text-xs text-on-surface-variant mt-1">{sub}</div>}
    </div>
  );
}
