interface EmptyProps {
  message?: string;
  action?: { label: string; onClick: () => void };
}

export default function Empty({ message = 'No data available', action }: EmptyProps) {
  return (
    <div className="flex flex-col items-center justify-center py-20 gap-3">
      <div className="w-12 h-12 rounded-full bg-surface-container-high flex items-center justify-center">
        <svg className="w-6 h-6 text-on-surface-variant" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
        </svg>
      </div>
      <span className="text-sm text-on-surface-variant">{message}</span>
      {action && (
        <button onClick={action.onClick} className="px-3 py-1.5 text-sm rounded bg-surface-container-high hover:bg-surface-container-highest border border-outline-variant text-on-surface transition-colors">
          {action.label}
        </button>
      )}
    </div>
  );
}
