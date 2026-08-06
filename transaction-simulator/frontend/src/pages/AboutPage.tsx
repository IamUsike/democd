import { Link } from "react-router-dom";

const RULES = [
  {
    id: "AMOUNT_THRESHOLD",
    defaultConfig: "amount > 10,000",
    meaning: "Flags a single transaction whose amount exceeds the configured threshold."
  },
  {
    id: "VELOCITY",
    defaultConfig: "more than 5 txns in 10 minutes",
    meaning: "Flags an account when too many transactions arrive inside the time window."
  },
  {
    id: "NEW_PAYEE",
    defaultConfig: "first payment to a payee",
    meaning: "Flags the first transaction from an account to a payee id that has never been seen before."
  },
  {
    id: "DAILY_LIMIT",
    defaultConfig: "daily sum > 50,000",
    meaning: "Flags when an account’s cumulative spend for the day crosses the daily limit."
  }
] as const;

const PACKS = [
  { id: "AMOUNT_THRESHOLD", tip: "One high-value transfer — fires Amount." },
  { id: "VELOCITY", tip: "Six quick posts on one account — fires Velocity." },
  { id: "NEW_PAYEE", tip: "Fresh payee id — fires New Payee." },
  { id: "DAILY_LIMIT", tip: "Burst summing past 50k under the amount threshold — fires Daily Limit." },
  {
    id: "MULTI_RULE",
    tip: "Pick ≥2 rules on the Simulator page; the pack builds a sequence that should trip all of them."
  },
  { id: "SOFT_TENANCY_MIX", tip: "BANK + MERCHANT normals — no alert; good for source filters." },
  { id: "MVP_SEED", tip: "Classic 3-txn demo path (2 quiet + 1 over threshold)." }
] as const;

export function AboutPage(): JSX.Element {
  return (
    <>
      <header className="page-header">
        <h1>About this simulator</h1>
        <p>
          Feed synthetic bank and merchant traffic into the Transaction Monitoring API so you can
          demo rule triggers without typing payloads by hand.
        </p>
      </header>

      <div className="about-stack">
        <section className="panel panel-pad">
          <h2>Quick start</h2>
          <ol>
            <li>
              Start the monitoring API on <span className="tag">:8081</span> and this simulator
              backend on <span className="tag">:8090</span>.
            </li>
            <li>
              Open the <Link to="/">Simulator</Link> page. Live metrics should poll without a
              network error.
            </li>
            <li>
              Click a <strong>scenario pack</strong> for a one-shot demo, or start{" "}
              <strong>continuous traffic</strong> for volume.
            </li>
            <li>
              Switch to the operator dashboard to acknowledge / investigate / close alerts — this
              app only posts transactions.
            </li>
          </ol>
        </section>

        <section className="panel panel-pad">
          <h2>Scenario packs</h2>
          <p>
            Deterministic sequences for live demos. Single-rule packs fire one expected alert type.
            <strong> Multi-rule hit</strong> lets you check which rules to combine (at least two);
            defaults to Amount + New Payee if you leave the defaults selected.
          </p>
          <ul>
            {PACKS.map((pack) => (
              <li key={pack.id}>
                <code>{pack.id}</code> — {pack.tip}
              </li>
            ))}
          </ul>
        </section>

        <section className="panel panel-pad">
          <h2>Continuous traffic controls</h2>
          <p>
            Use this for a live stream, not for precise rule demos. Metrics show HTTP success/fail
            of posts to the monitoring API — not alert outcomes.
          </p>
          <ul>
            <li>
              <strong>TPS / duration</strong> — how fast and how long to emit traffic.
            </li>
            <li>
              <strong>Mode NORMAL</strong> — amounts stay under the default amount threshold (~10k)
              so the stream stays quiet unless you add fraud mix.
            </li>
            <li>
              <strong>Mode FRAUD</strong> — every emission is a rule-aligned fraud pattern (full
              multi-txn sequences for velocity / daily limit / etc.). Fraud mix % is ignored.
            </li>
            <li>
              <strong>Fraud mix %</strong> — only applies when mode is <code>NORMAL</code>. Each
              emission has roughly that percent chance of being generated as a FRAUD pattern
              instead of a normal quiet txn. Example: <code>10</code> ≈ one in ten posts looks
              suspicious. This is <em>not</em> “% of alerts that fail” and not HTTP error
              injection — load/error rates stay in k6.
            </li>
            <li>
              <strong>Failed txn %</strong> — chance each continuous-traffic transaction is posted
              with <code>status: FAILED</code> instead of <code>COMPLETED</code>. Useful for
              transaction-list filters and KPIs by status. Scenario packs always stay{" "}
              <code>COMPLETED</code> so rule demos remain reliable. Note: the monitoring API still
              evaluates rules on FAILED txns the same way.
            </li>
            <li>
              <strong>Source filter</strong> — force <code>BANK</code> or <code>MERCHANT</code> on
              every generated txn (soft tenancy demo).
            </li>
          </ul>
        </section>

        <section className="panel panel-pad">
          <h2>Rules evaluated by the monitoring API</h2>
          <p>
            Defaults below match the monolith’s seed config. Operators can change thresholds in the
            rules UI; packs are tuned for those defaults.
          </p>
          <table className="rules-table">
            <thead>
              <tr>
                <th>Rule</th>
                <th>Default</th>
                <th>What it checks</th>
              </tr>
            </thead>
            <tbody>
              {RULES.map((rule) => (
                <tr key={rule.id}>
                  <td>{rule.id}</td>
                  <td>{rule.defaultConfig}</td>
                  <td>{rule.meaning}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section className="panel panel-pad">
          <h2>Soft tenancy</h2>
          <p>
            Every transaction carries <code>sourceType</code> (<code>BANK</code> /{" "}
            <code>MERCHANT</code>), <code>sourceId</code>, and <code>sourceName</code>. There is one
            database; feeds are distinguished by those fields — not separate schemas.
          </p>
        </section>

        <section className="panel panel-pad">
          <h2>What this tool does not do</h2>
          <ul>
            <li>Alert lifecycle (ack / investigate / close) — use the operator dashboard.</li>
            <li>HTTP failure injection or “% failed alerts” — use k6 for load/error evidence.</li>
            <li>Database reset / wipe — ops / seed scripts, not the simulator UI.</li>
          </ul>
        </section>
      </div>
    </>
  );
}
