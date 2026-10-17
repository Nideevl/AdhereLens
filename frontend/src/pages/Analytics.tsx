import { useState } from 'react';
import { get } from '../api';
import { useAuth } from '../auth';
import { BarList, LineChart } from '../components/charts';
import { ErrorBox, Field, Loading } from '../components/ui';
import { label, drugLabel } from '../format';
import { useLoad } from '../hooks';
import type {
  AnalyticsResult, DayPoint, DrugRow, HospitalRow, SideEffectRow, SummaryRow,
} from '../types';

type Scope = 'hospital' | 'network';

export default function Analytics() {
  const { auth } = useAuth();
  const role = auth?.role;
  const scopes: Scope[] = role === 'PLATFORM_ADMIN' ? ['network'] : role === 'HOSPITAL_ADMIN' ? ['hospital', 'network'] : ['hospital'];
  const [scope, setScope] = useState<Scope>(scopes[0]);
  const [careSetting, setCareSetting] = useState<'OUTPATIENT' | 'INPATIENT'>('OUTPATIENT');
  const [drug, setDrug] = useState('');

  const base = `/api/analytics/${scope}`;
  const drugQuery = drug ? `&drugName=${drug}` : '';
  const drugs = useLoad(() => get<AnalyticsResult<string>>(`${base}/drugs`), [scope]);
  const summary = useLoad(() => get<AnalyticsResult<SummaryRow>>(`${base}/summary${drug ? `?drugName=${drug}` : ''}`), [scope, drug]);
  const byDay = useLoad(() => get<AnalyticsResult<DayPoint>>(`${base}/adherence-by-day?careSetting=${careSetting}${drugQuery}`), [scope, careSetting, drug]);
  const byDrug = useLoad(() => get<AnalyticsResult<DrugRow>>(`${base}/adherence-by-drug?careSetting=${careSetting}`), [scope, careSetting]);
  const effects = useLoad(() => get<AnalyticsResult<SideEffectRow>>(`${base}/side-effects?${drug ? `drugName=${drug}` : ''}`), [scope, drug]);
  const compare = useLoad(
    () => (scope === 'network' && drug
      ? get<AnalyticsResult<HospitalRow>>(`/api/analytics/network/hospital-comparison?drugName=${drug}&careSetting=${careSetting}`)
      : Promise.resolve(null)),
    [scope, drug, careSetting],
  );

  const synthetic = [summary, byDay, byDrug, effects].some((r) => r.data?.includesSynthetic);
  const k = summary.data?.minGroupSize ?? 5;
  const hiddenNote = (rows: { suppressed: boolean }[]) => rows.some((r) => r.suppressed);
  const allHidden = (rows: { suppressed: boolean }[]) => rows.length > 0 && rows.every((r) => r.suppressed);
  const tooFew = (
    <div className="note">
      Nothing is shown here because every group has fewer than {k} patients, so it is hidden to protect privacy.
      {scope === 'hospital' ? ' Try "All hospitals" to pool more patients.' : ' Try another drug or care setting.'}
    </div>
  );

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>Analytics</h1>
          <p className="muted">
            De-identified numbers only. Any group with fewer than {k} patients is hidden. Inpatient and outpatient adherence are never mixed.
          </p>
        </div>
      </div>

      {synthetic && (
        <div className="banner">
          <strong>Synthetic data.</strong> These charts come from generated demo data that shows the pipeline working.
          They are not real findings.
        </div>
      )}

      {scopes.length > 1 && (
        <div className="tabs">
          <button className={scope === 'hospital' ? 'on' : ''} onClick={() => setScope('hospital')}>My hospital</button>
          <button className={scope === 'network' ? 'on' : ''} onClick={() => setScope('network')}>All hospitals (names hidden)</button>
        </div>
      )}

      <div className="card">
        <div className="grid">
          <Field label="Care setting">
            <select value={careSetting} onChange={(e) => setCareSetting(e.target.value as 'OUTPATIENT' | 'INPATIENT')}>
              <option value="OUTPATIENT">Outpatient</option>
              <option value="INPATIENT">Inpatient</option>
            </select>
          </Field>
          <Field label="Drug">
            <select value={drug} onChange={(e) => setDrug(e.target.value)}>
              <option value="">All drugs</option>
              {(drugs.data?.rows ?? []).map((d) => <option key={d} value={d}>{drugLabel(d)}</option>)}
            </select>
          </Field>
        </div>
      </div>

      <div className="card">
        <h2>Overview</h2>
        <ErrorBox message={summary.error} />
        <Loading loading={summary.loading} />
        {summary.data && (summary.data.rows.length === 0 ? <div className="empty">No dose data yet.</div> : (
          <div className="stat-grid">
            {summary.data.rows.map((r) => (
              <div className="stat" key={r.careSetting}>
                <div className="muted">{label(r.careSetting)}</div>
                {r.suppressed ? <div className="muted">Hidden (too few patients)</div> : (
                  <>
                    <div className="big">{r.takenRate}%</div>
                    <div className="muted"><small>taken, {r.doses} doses, {r.patients} patients</small></div>
                    <div className="muted"><small>{r.hit} on time, {r.delayed} late, {r.missed} missed, {r.refused} refused</small></div>
                  </>
                )}
              </div>
            ))}
          </div>
        ))}
      </div>

      <div className="card">
        <h2>Adherence by day of therapy</h2>
        <p className="muted">How many doses were taken (on time or late) on each day since the regimen started. {careSetting === 'OUTPATIENT' ? 'Outpatient drop-off is the pattern to watch.' : ''}</p>
        <ErrorBox message={byDay.error} />
        <Loading loading={byDay.loading} />
        {byDay.data && allHidden(byDay.data.rows) && tooFew}
        {byDay.data && !allHidden(byDay.data.rows) && (
          <>
            <LineChart
              xLabel="Day of therapy"
              yLabel="Adherence"
              series={[
                { name: 'Taken (on time or late)', color: '#0f766e', points: byDay.data.rows.map((r) => ({ x: r.day, y: r.takenRate })) },
                { name: 'On time', color: '#c2410c', points: byDay.data.rows.map((r) => ({ x: r.day, y: r.onTimeRate })) },
              ]}
            />
            {hiddenNote(byDay.data.rows) && <p className="muted"><small>Some days are hidden because too few patients contributed.</small></p>}
          </>
        )}
      </div>

      <div className="card">
        <h2>Adherence by drug</h2>
        <ErrorBox message={byDrug.error} />
        <Loading loading={byDrug.loading} />
        {byDrug.data && allHidden(byDrug.data.rows) && tooFew}
        {byDrug.data && !allHidden(byDrug.data.rows) && (
          <BarList bars={byDrug.data.rows.map((r) => ({
            label: drugLabel(r.drugName), value: r.suppressed ? null : r.takenRate,
            note: r.suppressed ? undefined : `${r.patients} patients, ${r.doses} doses`,
          }))} />
        )}
      </div>

      <div className="card">
        <h2>Side effects by age and gender</h2>
        <p className="muted">Share of each group with the side effect reported by a doctor or nurse. {drug ? '' : 'Choose a drug above to focus on one.'}</p>
        <ErrorBox message={effects.error} />
        <Loading loading={effects.loading} />
        {effects.data && effects.data.rows.every((r) => r.suppressed) && (
          <div className="note">
            No side effect was reported by {k} or more patients in any age and gender group, so nothing is shown.
            {scope === 'hospital' ? ' Try "All hospitals", or pick a drug that has more reports.' : ' Try another drug.'}
          </div>
        )}
        {effects.data && !effects.data.rows.every((r) => r.suppressed) && (
          <BarList bars={effects.data.rows.filter((r) => !r.suppressed).slice(0, 12).map((r) => ({
            label: `${label(r.sideEffect)}: ${r.ageBucket}, ${label(r.gender).toLowerCase()}`,
            value: r.ratePercent,
            note: `${r.patientsReporting} of ${r.cohortPatients} patients`,
          }))} />
        )}
        {effects.data && !effects.data.rows.every((r) => r.suppressed) && hiddenNote(effects.data.rows) && <p className="muted"><small>Groups with too few patients are not shown.</small></p>}
      </div>

      {scope === 'network' && (
        <div className="card">
          <h2>Same drug across hospitals</h2>
          <p className="muted">Hospitals appear only as a short code. Your own hospital is highlighted.</p>
          {!drug && <div className="empty">Choose a drug above to compare hospitals.</div>}
          <ErrorBox message={compare.error} />
          {compare.data && (
            <BarList bars={compare.data.rows.map((r) => ({
              label: `${r.mine ? 'Your hospital' : r.label}, ${r.doseAmount} ${r.doseUnit}`,
              value: r.suppressed ? null : r.takenRate,
              note: r.suppressed ? undefined : `${r.patients} patients`,
              highlight: r.mine,
            }))} />
          )}
        </div>
      )}
    </div>
  );
}
