/* @vitest-environment jsdom */
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import AiJourneyAssistant from './AiJourneyAssistant'

describe('AiJourneyAssistant', () => {
  it('generates a safe DSL preview with risks and trace references without enabling publish', async () => {
    render(<AiJourneyAssistant />)

    expect(screen.getByRole('heading', { name: 'AI Journey Assistant' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Generate draft preview' })).toBeDisabled()
    expect(screen.getByText('Draft preview pending')).toBeInTheDocument()
    expect(screen.getByText('Playground handoff uses the mock AI provider and keeps output in draft preview.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Publish disabled for playground preview' })).toBeDisabled()

    fireEvent.change(screen.getByLabelText('Journey brief'), {
      target: { value: 'Welcome new users with a coupon after registration and explain failed delivery traces.' },
    })

    const generateButton = screen.getByRole('button', { name: 'Generate draft preview' })
    expect(generateButton).toBeEnabled()
    fireEvent.click(generateButton)

    expect(screen.getByRole('button', { name: 'Generating preview' })).toBeDisabled()
    expect(screen.getByText('Mock provider is preparing a draft preview.')).toBeInTheDocument()

    await waitFor(() => expect(screen.getByText('Draft preview ready')).toBeInTheDocument())

    expect(screen.getByText('apiVersion: canvas/v1')).toBeInTheDocument()
    expect(screen.getByText('kind: Journey')).toBeInTheDocument()
    expect(screen.getByText('metadata.name: ai-welcome-new-users-with')).toBeInTheDocument()
    expect(screen.getByText('provider: mock-ai')).toBeInTheDocument()
    expect(screen.getByText('mode: draft-preview-only')).toBeInTheDocument()
    expect(screen.getByText('Journey semantics')).toBeInTheDocument()
    expect(screen.getByText('webhook trigger user.registered')).toBeInTheDocument()
    expect(screen.getByText('approval gate before coupon grant')).toBeInTheDocument()
    expect(screen.getByText('risk-check verifies frequency cap and coupon budget')).toBeInTheDocument()
    expect(screen.getByText('Risk findings')).toBeInTheDocument()
    expect(screen.getByText('MEDIUM: Confirm approval owner before moving out of draft.')).toBeInTheDocument()
    expect(screen.getByText('LOW: Validate coupon inventory cap with campaign operations.')).toBeInTheDocument()
    expect(screen.getByText('Trace references')).toBeInTheDocument()
    expect(screen.getByText('trace:mock-registration-delivery-timeout')).toBeInTheDocument()
    expect(screen.getByText('trace:mock-coupon-budget-check')).toBeInTheDocument()
    expect(screen.getByText('risk:audit:mock-new-user-welcome')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Publish disabled for playground preview' })).toBeDisabled()
    expect(screen.getByText('Publish boundary: disabled until live draft, publish, trace, and risk APIs are wired.')).toBeInTheDocument()
  })
})
