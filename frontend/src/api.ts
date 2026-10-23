// Thin fetch wrapper over the spec 001/002 API contracts. The screen never changes the API (R-UI-11).

export type Link = {
  id: number;
  url: string;
  memo: string | null;
  tags: string[];
  createdAt: string;
  read: boolean;
};

export type LinkPage = {
  items: Link[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

export type FieldError = { field: string; message: string };

export type ApiFailure =
  // 400: errors[] may be empty (e.g. malformed JSON has no errors).
  | { kind: 'validation'; errors: FieldError[]; detail: string | null }
  | { kind: 'conflict'; detail: string | null }
  | { kind: 'notFound'; detail: string | null }
  | { kind: 'http'; status: number; detail: string | null }
  | { kind: 'network' };

export type ApiResult<T> = { ok: true; data: T } | { ok: false; error: ApiFailure };

async function request<T>(input: string, init?: RequestInit): Promise<ApiResult<T>> {
  let response: Response;
  try {
    response = await fetch(input, init);
  } catch {
    return { ok: false, error: { kind: 'network' } };
  }

  let body: unknown = null;
  try {
    body = await response.json();
  } catch {
    body = null;
  }

  if (response.ok) {
    if (body === null) return { ok: false, error: { kind: 'http', status: response.status, detail: null } };
    return { ok: true, data: body as T };
  }

  const problem = (body ?? {}) as { detail?: unknown; errors?: unknown };
  const detail = typeof problem.detail === 'string' ? problem.detail : null;
  switch (response.status) {
    case 400: {
      const errors = Array.isArray(problem.errors)
        ? problem.errors.filter(
            (e): e is FieldError =>
              typeof e === 'object' && e !== null && typeof e.field === 'string' && typeof e.message === 'string',
          )
        : [];
      return { ok: false, error: { kind: 'validation', errors, detail } };
    }
    case 404:
      return { ok: false, error: { kind: 'notFound', detail } };
    case 409:
      return { ok: false, error: { kind: 'conflict', detail } };
    default:
      return { ok: false, error: { kind: 'http', status: response.status, detail } };
  }
}

const JSON_HEADERS = { 'Content-Type': 'application/json; charset=UTF-8' };

export type LinkOrder = 'newest' | 'oldest';

export function listLinks({
  page,
  tag,
  order,
  unread,
}: {
  page: number;
  tag: string | null;
  order: LinkOrder;
  unread: boolean;
}): Promise<ApiResult<LinkPage>> {
  const params = new URLSearchParams({ page: String(page), order });
  // Blank tags are never sent: the API answers 400 for them (R-UI-07).
  const trimmed = tag?.trim();
  if (trimmed) params.set('tag', trimmed);
  if (unread) params.set('unread', 'true');
  return request<LinkPage>(`/api/links?${params.toString()}`);
}

export function saveLink({
  url,
  memo,
  tags,
}: {
  url: string;
  memo: string | null;
  tags: string[];
}): Promise<ApiResult<Link>> {
  return request<Link>('/api/links', {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({ url, memo, tags }),
  });
}

export function markRead(id: number, read: boolean): Promise<ApiResult<Link>> {
  return request<Link>(`/api/links/${encodeURIComponent(String(id))}/read`, {
    method: 'PUT',
    headers: JSON_HEADERS,
    body: JSON.stringify({ read }),
  });
}

export function failureMessage(error: ApiFailure): string {
  if (error.kind === 'network') return '서버에 연결할 수 없습니다.';
  if (error.kind === 'validation' && error.errors.length > 0) {
    return error.errors.map((e) => e.message).join(', ');
  }
  return error.detail ?? '요청을 처리하지 못했습니다.';
}
