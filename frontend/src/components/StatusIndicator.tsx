type StatusTone = 'open' | 'ack' | 'closed';

function statusTone(status: string): StatusTone {
  const normalized = status.toUpperCase();
  if (normalized === 'OPEN' || normalized === 'FAILED' || normalized === 'REJECTED') {
    return 'open';
  }
  if (
    normalized === 'ACKNOWLEDGED' ||
    normalized === 'INVESTIGATING' ||
    normalized === 'PENDING' ||
    normalized === 'PROCESSING'
  ) {
    return 'ack';
  }
  return 'closed';
}

type StatusIndicatorProps = {
  status: string;
};

export function StatusIndicator({ status }: StatusIndicatorProps) {
  return (
    <span className="status">
      <span className={`status-dot ${statusTone(status)}`} aria-hidden="true" />
      {status}
    </span>
  );
}
