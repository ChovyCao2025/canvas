import { describe, expect, it, vi } from 'vitest'
import { createSaveQueue, type SaveQueueState } from './saveQueue'

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>(next => {
    resolve = next
  })
  return { promise, resolve }
}

describe('saveQueue', () => {
  it('coalesces rapid edits into the latest payload', async () => {
    const save = vi.fn().mockResolvedValue({ version: 2 })
    const queue = createSaveQueue({ save, maxAttempts: 3, baseDelayMs: 1 })

    const first = queue.enqueue({ version: 1, nodes: ['old'] })
    const second = queue.enqueue({ version: 1, nodes: ['latest'] })
    const results = await Promise.all([first, second])

    expect(save).toHaveBeenCalledTimes(1)
    expect(save).toHaveBeenCalledWith({ version: 1, nodes: ['latest'] }, expect.any(AbortSignal))
    expect(results.map(result => result.status)).toEqual(['saved', 'saved'])
  })

  it('flushes the latest pending payload after an active save finishes', async () => {
    const firstSave = deferred<{ version: number }>()
    const save = vi.fn()
      .mockReturnValueOnce(firstSave.promise)
      .mockResolvedValueOnce({ version: 3 })
    const queue = createSaveQueue({ save, maxAttempts: 3, baseDelayMs: 1 })

    const first = queue.enqueue({ version: 1, nodes: ['first'] })
    await Promise.resolve()
    const second = queue.enqueue({ version: 1, nodes: ['latest'] })

    firstSave.resolve({ version: 2 })

    await Promise.all([first, second])

    expect(save).toHaveBeenCalledTimes(2)
    expect(save).toHaveBeenNthCalledWith(2, { version: 1, nodes: ['latest'] }, expect.any(AbortSignal))
  })

  it('retries transient errors with exponential backoff then recovers', async () => {
    const states: SaveQueueState[] = []
    const delays: number[] = []
    const save = vi.fn()
      .mockRejectedValueOnce({ retryable: true })
      .mockRejectedValueOnce({ status: 503 })
      .mockResolvedValueOnce({ version: 3 })
    const queue = createSaveQueue({
      save,
      maxAttempts: 3,
      baseDelayMs: 10,
      sleep: async delayMs => { delays.push(delayMs) },
      onStateChange: state => states.push(state),
    })

    const result = await queue.enqueue({ version: 2 })

    expect(result.status).toBe('saved')
    expect(result.attempts).toBe(3)
    expect(save).toHaveBeenCalledTimes(3)
    expect(delays).toEqual([10, 20])
    expect(states.some(state => state.status === 'retrying' && state.attempts === 1)).toBe(true)
    expect(queue.getState().status).toBe('saved')
  })

  it('returns conflict without clearing the local payload', async () => {
    const save = vi.fn().mockRejectedValue({ status: 409, serverVersion: 5 })
    const queue = createSaveQueue({ save, maxAttempts: 3, baseDelayMs: 1 })

    const result = await queue.enqueue({ version: 4, nodes: ['local'] })

    expect(result).toMatchObject({
      status: 'conflict',
      serverVersion: 5,
      payload: { version: 4, nodes: ['local'] },
    })
    expect(save).toHaveBeenCalledTimes(1)
    expect(queue.getState()).toMatchObject({
      status: 'conflict',
      serverVersion: 5,
      pending: false,
    })
  })

  it('stops after the configured retry budget is exhausted', async () => {
    const delays: number[] = []
    const save = vi.fn().mockRejectedValue({ retryable: true })
    const queue = createSaveQueue({
      save,
      maxAttempts: 2,
      baseDelayMs: 25,
      sleep: async delayMs => { delays.push(delayMs) },
    })

    const result = await queue.enqueue({ version: 6 })

    expect(result).toMatchObject({
      status: 'failed',
      attempts: 2,
      payload: { version: 6 },
    })
    expect(save).toHaveBeenCalledTimes(2)
    expect(delays).toEqual([25])
    expect(queue.getState().status).toBe('failed')
  })
})
