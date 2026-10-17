import type { ReactNode } from 'react';

export interface Point { x: number; y: number | null }
export interface Series { name: string; color: string; points: Point[] }

/** A simple line chart on a 0 to 100 scale. A null y leaves a gap (a hidden small group). */
export function LineChart({ series, xLabel, yLabel }: { series: Series[]; xLabel: string; yLabel: string }) {
  const W = 640, H = 260, L = 46, R = 14, T = 12, B = 40;
  const xs = series.flatMap((s) => s.points.map((p) => p.x));
  if (xs.length === 0) return <div className="empty">No data for this selection.</div>;
  const xMin = Math.min(...xs), xMax = Math.max(...xs);
  const sx = (x: number) => L + (xMax === xMin ? 0 : ((x - xMin) / (xMax - xMin)) * (W - L - R));
  const sy = (y: number) => T + (1 - y / 100) * (H - T - B);

  const paths = series.map((s) => {
    let d = '';
    let pen = false;
    s.points.forEach((p) => {
      if (p.y === null) { pen = false; return; }
      d += `${pen ? 'L' : 'M'}${sx(p.x).toFixed(1)},${sy(p.y).toFixed(1)} `;
      pen = true;
    });
    return d;
  });
  const step = Math.max(1, Math.ceil((xMax - xMin + 1) / 10));
  const ticks: number[] = [];
  for (let x = xMin; x <= xMax; x += step) ticks.push(x);

  return (
    <div>
      <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 'auto' }} role="img" aria-label={`${yLabel} by ${xLabel}`}>
        {[0, 25, 50, 75, 100].map((g) => (
          <g key={g}>
            <line x1={L} x2={W - R} y1={sy(g)} y2={sy(g)} stroke="#e3e8ee" />
            <text x={L - 8} y={sy(g) + 4} textAnchor="end" fontSize="11" fill="#627080">{g}%</text>
          </g>
        ))}
        {ticks.map((x) => (
          <text key={x} x={sx(x)} y={H - B + 16} textAnchor="middle" fontSize="11" fill="#627080">{x}</text>
        ))}
        <text x={(L + W - R) / 2} y={H - 6} textAnchor="middle" fontSize="12" fill="#627080">{xLabel}</text>
        {paths.map((d, i) => <path key={series[i].name} d={d} fill="none" stroke={series[i].color} strokeWidth="2.5" />)}
      </svg>
      <div className="legend">
        {series.map((s) => (
          <span key={s.name}><i style={{ background: s.color }} /> {s.name}</span>
        ))}
      </div>
    </div>
  );
}

export interface Bar { label: ReactNode; value: number | null; note?: string; highlight?: boolean }

/** Horizontal bars on a 0 to 100 scale. A null value shows as hidden. */
export function BarList({ bars }: { bars: Bar[] }) {
  if (bars.length === 0) return <div className="empty">No data for this selection.</div>;
  return (
    <div className="bars">
      {bars.map((b, i) => (
        <div className="bar-row" key={i}>
          <div className="bar-label">{b.label}</div>
          <div className="bar-track">
            {b.value !== null && <div className={`bar-fill ${b.highlight ? 'hl' : ''}`} style={{ width: `${Math.min(100, b.value)}%` }} />}
          </div>
          <div className="bar-value">{b.value !== null ? `${b.value}%` : 'hidden'}</div>
          {b.note && <div className="bar-note muted">{b.note}</div>}
        </div>
      ))}
    </div>
  );
}
