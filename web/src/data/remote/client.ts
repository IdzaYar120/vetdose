import type { SyncResponseDto } from "./dto"

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = "ApiError"
    this.status = status
  }
}

/** `GET /api/v1/sync` — the only endpoint the web client calls: search and
 * dose calculation both run entirely offline against Dexie, the same
 * offline-first design as the Android app's Retrofit `VetDoseApi`. */
export async function fetchSync(baseUrl: string, since: string | null): Promise<SyncResponseDto> {
  const normalizedBase = baseUrl.endsWith("/") ? baseUrl : `${baseUrl}/`
  const url = new URL("api/v1/sync", normalizedBase)
  if (since !== null) {
    url.searchParams.set("since", since)
  }

  const response = await fetch(url)
  if (!response.ok) {
    throw new ApiError(response.status, `Запит синхронізації завершився помилкою: ${String(response.status)}`)
  }
  return (await response.json()) as SyncResponseDto
}
