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
          <h2>Scenario packs vs continuous traffic</h2>
          <p>
            <strong>Packs</strong> are deterministic sequences aligned to a single rule (or a quiet
            tenancy mix). Use them in live demos.
          </p>
          <p>
            <strong>Traffic</strong> is TPS-paced. NORMAL stays under the amount threshold so it
            stays quiet; FRAUD emits full multi-txn patterns (velocity, daily limit, new payee,
            high amount). Optional source filter and fraud-mix % shape the stream.
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
      </div>
    </>
  );
}
