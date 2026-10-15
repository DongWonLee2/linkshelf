import { useEffect, useRef, useState } from 'react';
import { type Link, type LinkPage, failureMessage, markRead } from './api';

// Local time, YYYY-MM-DD HH:mm (clarify Q2).
export function formatCreatedAt(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

function LinkItem({ link }: { link: Link }) {
  const [read, setRead] = useState(link.read);
  const [error, setError] = useState<string | null>(null);
  // PUTs are serialized: one request at a time, and after it settles the latest wanted state is sent if it differs.
  // So the server sees changes in order, the last change wins, and a failure reverts to the server-confirmed value.
  const confirmed = useRef(link.read);
  const desired = useRef(link.read);
  const sending = useRef(false);

  useEffect(() => {
    if (sending.current) return;
    confirmed.current = link.read;
    desired.current = link.read;
    setRead(link.read);
    setError(null);
  }, [link]);

  async function toggle(next: boolean) {
    desired.current = next;
    setRead(next);
    setError(null);
    if (sending.current) return;
    sending.current = true;
    while (desired.current !== confirmed.current) {
      const result = await markRead(link.id, desired.current);
      if (result.ok) {
        confirmed.current = result.data.read;
      } else {
        // R-UI-10: revert to the last value the server confirmed and explain on this item.
        desired.current = confirmed.current;
        setError(`읽음 표시를 바꾸지 못했습니다: ${failureMessage(result.error)}`);
      }
    }
    sending.current = false;
    setRead(confirmed.current);
  }

  return (
    <li className={read ? 'link-item read' : 'link-item'} data-testid="link-item" data-link-id={link.id}>
      <label className="read-toggle">
        <input
          data-testid="link-item-read-toggle"
          type="checkbox"
          checked={read}
          onChange={(e) => void toggle(e.target.checked)}
          aria-label="읽음"
        />
      </label>
      <div className="link-body">
        <a data-testid="link-item-url" href={link.url} target="_blank" rel="noopener noreferrer">
          {link.url}
        </a>
        {link.memo !== null && link.memo !== '' && (
          <p className="link-memo" data-testid="link-item-memo">
            {link.memo}
          </p>
        )}
        <div className="link-meta">
          {link.tags.length > 0 && (
            <ul className="tags" aria-label="태그">
              {link.tags.map((tag) => (
                <li key={tag} className="tag" data-testid="link-item-tag">
                  {tag}
                </li>
              ))}
            </ul>
          )}
          <time data-testid="link-item-created-at" dateTime={link.createdAt}>
            {formatCreatedAt(link.createdAt)}
          </time>
        </div>
        {error && (
          <p className="field-error" data-testid="link-item-error" role="alert">
            {error}
          </p>
        )}
      </div>
    </li>
  );
}

type Props = {
  data: LinkPage | null;
  error: string | null;
  busy: boolean;
  onPage: (page: number) => void;
};

export function LinkList({ data, error, busy, onPage }: Props) {
  if (error) {
    return (
      <p className="notice error" data-testid="list-error" role="alert">
        목록을 불러오지 못했습니다: {error}
      </p>
    );
  }
  if (data === null) return <p className="notice">불러오는 중…</p>;

  const empty = data.items.length === 0;
  return (
    <section className="link-list" data-testid="link-list" aria-label="저장한 링크">
      {empty ? (
        <p className="notice" data-testid="link-list-empty">
          보여 줄 링크가 없습니다.
        </p>
      ) : (
        <>
          <ul className="items">
            {data.items.map((link) => (
              <LinkItem key={link.id} link={link} />
            ))}
          </ul>
          <nav className="pager" aria-label="쪽 넘김">
            <button
              data-testid="link-list-prev"
              type="button"
              disabled={busy || data.page <= 0}
              onClick={() => onPage(data.page - 1)}
            >
              이전
            </button>
            <span data-testid="link-list-page-info">
              {data.page + 1} / {data.totalPages}
            </span>
            <button
              data-testid="link-list-next"
              type="button"
              disabled={busy || !data.hasNext}
              onClick={() => onPage(data.page + 1)}
            >
              다음
            </button>
          </nav>
        </>
      )}
    </section>
  );
}
