/* @vitest-environment jsdom */
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'

import PublicMarketingFormPage from './index'
import { marketingFormsApi } from '../../services/marketingFormsApi'

vi.mock('../../services/marketingFormsApi', () => ({
  marketingFormsApi: {
    publicForm: vi.fn(),
    publicSubmit: vi.fn(),
  },
}))

describe('PublicMarketingFormPage', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('renders first-run catalog fields when the backend returns fieldSchema', async () => {
    vi.mocked(marketingFormsApi.publicForm).mockResolvedValue({
      code: 0,
      message: 'success',
      data: {
        publicKey: 'lead-capture',
        name: 'Lead capture',
        status: 'ACTIVE',
        fieldSchema: ['email', 'company', 'message'],
        successMessage: 'Thanks, we received your request.',
      },
    } as any)

    render(
      <MemoryRouter initialEntries={['/public/forms/lead-capture']}>
        <Routes>
          <Route path="/public/forms/:publicKey" element={<PublicMarketingFormPage />} />
        </Routes>
      </MemoryRouter>,
    )

    await waitFor(() => expect(marketingFormsApi.publicForm).toHaveBeenCalledWith('lead-capture'))
    expect(screen.getByLabelText('email')).toBeInTheDocument()
    expect(screen.getByLabelText('company')).toBeInTheDocument()
    expect(screen.getByLabelText('message')).toBeInTheDocument()
  })
})
