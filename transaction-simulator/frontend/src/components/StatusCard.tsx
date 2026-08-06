interface StatusCardProps {
  title: string;
  value: string | number;
  status?: "normal" | "success" | "warning" | "error";
}

function statusColor(status: NonNullable<StatusCardProps["status"]>): string {
  switch (status) {
    case "success":
      return "#166534";
    case "warning":
      return "#92400e";
    case "error":
      return "#991b1b";
    default:
      return "#1f2937";
  }
}

export function StatusCard({ title, value, status = "normal" }: StatusCardProps): JSX.Element {
  return (
    <article
      style={{
        border: "1px solid #e5e7eb",
        borderRadius: "8px",
        padding: "12px",
        minWidth: "180px"
      }}
    >
      <p style={{ margin: 0, fontSize: "0.875rem", color: "#6b7280" }}>{title}</p>
      <p
        style={{
          margin: "8px 0 0",
          fontSize: "1.25rem",
          fontWeight: 600,
          color: statusColor(status)
        }}
      >
        {value}
      </p>
    </article>
  );
}

