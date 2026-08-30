import { api } from './client'
import type { CopilotRequest, CopilotResponse } from '../types/api'

export const queryCopilot = async (request: CopilotRequest): Promise<CopilotResponse> =>
  (await api.post<CopilotResponse>('/api/copilot/query', request)).data
