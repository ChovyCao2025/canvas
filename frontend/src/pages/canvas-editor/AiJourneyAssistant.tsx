import { useMemo, useState } from 'react'
import type { CSSProperties } from 'react'

type PreviewStatus = 'idle' | 'generating' | 'ready'

interface DraftPreview {
  provider: 'mock-ai'
  mode: 'draft-preview-only'
  metadataName: string
  journeySemantics: string[]
  riskFindings: string[]
  traceReferences: string[]
  riskReferences: string[]
}

const stopWords = new Set(['a', 'after', 'and', 'the', 'for', 'from', 'into'])

function buildMetadataName(brief: string): string {
  const words = brief
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, '')
    .split(/\s+/)
    .filter((word) => word && !stopWords.has(word))
    .slice(0, 4)

  return `ai-${words.join('-') || 'journey-draft'}`
}

function buildMockPreview(brief: string): DraftPreview {
  return {
    provider: 'mock-ai',
    mode: 'draft-preview-only',
    metadataName: buildMetadataName(brief),
    journeySemantics: [
      'webhook trigger user.registered',
      'approval gate before coupon grant',
      'risk-check verifies frequency cap and coupon budget',
      'message branch exits to end on delivery failure',
    ],
    riskFindings: [
      'MEDIUM: Confirm approval owner before moving out of draft.',
      'LOW: Validate coupon inventory cap with campaign operations.',
      'LOW: Review touch frequency against active lifecycle journeys.',
    ],
    traceReferences: [
      'trace:mock-registration-delivery-timeout',
      'trace:mock-coupon-budget-check',
    ],
    riskReferences: [
      'risk:audit:mock-new-user-welcome',
    ],
  }
}

export default function AiJourneyAssistant() {
  const [brief, setBrief] = useState('')
  const [status, setStatus] = useState<PreviewStatus>('idle')
  const [preview, setPreview] = useState<DraftPreview | null>(null)

  const canGenerate = brief.trim().length > 0 && status !== 'generating'
  const statusLabel = useMemo(() => {
    if (status === 'generating') return 'Mock provider is preparing a draft preview.'
    if (status === 'ready') return 'Draft preview ready'
    return 'Draft preview pending'
  }, [status])

  function handleGenerate() {
    if (!canGenerate) return

    setStatus('generating')
    setPreview(null)
    window.setTimeout(() => {
      setPreview(buildMockPreview(brief))
      setStatus('ready')
    }, 0)
  }

  return (
    <section aria-labelledby="ai-journey-assistant-title" style={styles.shell}>
      <div style={styles.header}>
        <div>
          <p style={styles.kicker}>Mock provider</p>
          <h2 id="ai-journey-assistant-title" style={styles.title}>AI Journey Assistant</h2>
        </div>
        <span style={styles.badge}>Preview only</span>
      </div>

      <label htmlFor="ai-journey-brief" style={styles.label}>Journey brief</label>
      <textarea
        id="ai-journey-brief"
        value={brief}
        onChange={(event) => setBrief(event.target.value)}
        placeholder="Describe the audience, trigger, offer, controls, and trace context."
        rows={4}
        style={styles.textarea}
      />

      <div style={styles.actions}>
        <button
          type="button"
          onClick={handleGenerate}
          disabled={!canGenerate}
          style={canGenerate ? styles.primaryButton : styles.disabledButton}
        >
          {status === 'generating' ? 'Generating preview' : 'Generate draft preview'}
        </button>
        <button type="button" disabled style={styles.disabledButton}>
          Publish disabled for playground preview
        </button>
      </div>

      <p aria-live="polite" style={styles.status}>{statusLabel}</p>
      <p style={styles.safety}>Playground handoff uses the mock AI provider and keeps output in draft preview.</p>
      <p style={styles.safety}>Publish boundary: disabled until live draft, publish, trace, and risk APIs are wired.</p>

      {preview && (
        <div style={styles.previewGrid}>
          <section style={styles.panel} aria-label="DSL draft summary">
            <h3 style={styles.panelTitle}>DSL draft summary</h3>
            <dl style={styles.definitionList}>
              <div>
                <dt>apiVersion</dt>
                <dd>apiVersion: canvas/v1</dd>
              </div>
              <div>
                <dt>kind</dt>
                <dd>kind: Journey</dd>
              </div>
              <div>
                <dt>metadata.name</dt>
                <dd>{`metadata.name: ${preview.metadataName}`}</dd>
              </div>
              <div>
                <dt>provider</dt>
                <dd>{`provider: ${preview.provider}`}</dd>
              </div>
              <div>
                <dt>mode</dt>
                <dd>{`mode: ${preview.mode}`}</dd>
              </div>
            </dl>
          </section>

          <PreviewList title="Journey semantics" items={preview.journeySemantics} />
          <PreviewList title="Risk findings" items={preview.riskFindings} />
          <PreviewList title="Trace references" items={preview.traceReferences} />
          <PreviewList title="Risk references" items={preview.riskReferences} />
        </div>
      )}
    </section>
  )
}

function PreviewList({ title, items }: { title: string; items: string[] }) {
  return (
    <section style={styles.panel} aria-label={title}>
      <h3 style={styles.panelTitle}>{title}</h3>
      <ul style={styles.list}>
        {items.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
    </section>
  )
}

const styles = {
  shell: {
    border: '1px solid #d8dee8',
    borderRadius: 8,
    padding: 20,
    background: '#ffffff',
    color: '#1f2937',
    display: 'grid',
    gap: 14,
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    gap: 16,
    alignItems: 'flex-start',
  },
  kicker: {
    margin: 0,
    color: '#5b6472',
    fontSize: 12,
    fontWeight: 700,
    textTransform: 'uppercase',
    letterSpacing: 0,
  },
  title: {
    margin: '4px 0 0',
    fontSize: 22,
    lineHeight: 1.25,
  },
  badge: {
    border: '1px solid #b7c5d8',
    borderRadius: 999,
    color: '#31516f',
    padding: '4px 10px',
    fontSize: 12,
    fontWeight: 700,
    whiteSpace: 'nowrap',
  },
  label: {
    fontWeight: 700,
  },
  textarea: {
    width: '100%',
    boxSizing: 'border-box',
    border: '1px solid #c8d1df',
    borderRadius: 6,
    padding: 10,
    resize: 'vertical',
    font: 'inherit',
  },
  actions: {
    display: 'flex',
    gap: 10,
    flexWrap: 'wrap',
  },
  primaryButton: {
    border: '1px solid #1f6feb',
    borderRadius: 6,
    background: '#1f6feb',
    color: '#ffffff',
    fontWeight: 700,
    padding: '8px 12px',
    cursor: 'pointer',
  },
  disabledButton: {
    border: '1px solid #c8d1df',
    borderRadius: 6,
    background: '#eef2f7',
    color: '#697386',
    fontWeight: 700,
    padding: '8px 12px',
    cursor: 'not-allowed',
  },
  status: {
    margin: 0,
    color: '#344054',
    fontWeight: 700,
  },
  safety: {
    margin: 0,
    color: '#5b6472',
  },
  previewGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
    gap: 12,
  },
  panel: {
    border: '1px solid #d8dee8',
    borderRadius: 8,
    padding: 12,
    background: '#f8fafc',
  },
  panelTitle: {
    margin: '0 0 8px',
    fontSize: 15,
  },
  definitionList: {
    margin: 0,
    display: 'grid',
    gap: 8,
  },
  list: {
    margin: 0,
    paddingLeft: 18,
    display: 'grid',
    gap: 6,
  },
} satisfies Record<string, CSSProperties>
