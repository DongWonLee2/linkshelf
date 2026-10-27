import { useCallback, useEffect, useRef, useState } from 'react';
import { type LinkOrder, type LinkPage, failureMessage, listLinks } from './api';
import { LinkForm } from './LinkForm';
import { LinkList } from './LinkList';
import { SortSelect } from './SortSelect';
import { TagFilter } from './TagFilter';
import { UnreadToggle } from './UnreadToggle';
import './styles.css';

// tagFromFilter: the tag was applied or cleared by the filter and has not been shown yet (R-UI-08).
type ListConditions = { tag: string | null; order: LinkOrder; unread: boolean; tagFromFilter: boolean };

const DEFAULT_CONDITIONS: ListConditions = { tag: null, order: 'newest', unread: false, tagFromFilter: false };

export function App() {
  const [data, setData] = useState<LinkPage | null>(null);
  const [tag, setTag] = useState<string | null>(null);
  const [filterInput, setFilterInput] = useState('');
  const [order, setOrder] = useState<LinkOrder>('newest');
  const [unread, setUnread] = useState(false);
  const [listError, setListError] = useState<string | null>(null);
  const [filterError, setFilterError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  // Only the newest request may update the screen.
  const latest = useRef(0);
  // A new request must keep a filter, clear or order change that is still loading, so it reads the last chosen conditions.
  const requested = useRef<ListConditions>(DEFAULT_CONDITIONS);
  const requestedPage = useRef(0);
  const shownTag = useRef<string | null>(null);
  const browseTop = useRef<HTMLElement>(null);

  // Resolves true when this request's page was shown.
  const load = useCallback(
    async (page: number, next: ListConditions) => {
      const mine = ++latest.current;
      requested.current = next;
      requestedPage.current = page;
      setBusy(true);
      const result = await listLinks({ page, tag: next.tag, order: next.order, unread: next.unread });
      if (mine !== latest.current) return false;
      setBusy(false);

      if (result.ok) {
        setData(result.data);
        shownTag.current = next.tag;
        requested.current = { ...next, tagFromFilter: false };
        setTag(next.tag);
        setListError(null);
        setFilterError(null);
        return true;
      }
      // R-UI-24: a failure keeps the chosen order and unread; a tag applies only once its list is shown (R-UI-08).
      requested.current = { ...requested.current, tag: shownTag.current, tagFromFilter: false };
      const error = result.error;
      if (next.tagFromFilter && error.kind === 'validation') {
        // R-UI-08: keep the previous list and filter, show the error by the filter input,
        // also when an order or unread change sent the filter's tag before the filter's own answer came back.
        const tagErrors = error.errors.filter((e) => e.field === 'tag').map((e) => e.message);
        setFilterError(tagErrors.length > 0 ? tagErrors.join(', ') : failureMessage(error));
        return false;
      }
      setListError(failureMessage(error));
      return false;
    },
    [],
  );

  // Set when paging succeeded; the effect below acts once the new page has rendered.
  const pagedTo = useRef(false);

  const goToPage = async (page: number) => {
    if (await load(page, { ...requested.current, tag, tagFromFilter: false })) pagedTo.current = true;
  };

  useEffect(() => {
    if (!pagedTo.current) return;
    pagedTo.current = false;
    // R-UI-16: bring the list's top (and so the new first item) into view.
    browseTop.current?.scrollIntoView({ block: 'start' });
    // R-UI-17: move keyboard focus off the pager to the new page's first link.
    browseTop.current?.querySelector<HTMLAnchorElement>('.link-body a')?.focus({ preventScroll: true });
  }, [data]);

  useEffect(() => {
    void load(0, DEFAULT_CONDITIONS);
  }, [load]);

  const applyFilter = (input: string) => void load(0, { ...requested.current, tag: input.trim(), tagFromFilter: true });

  const clearFilter = () => {
    setFilterInput('');
    void load(0, { ...requested.current, tag: null, tagFromFilter: true });
  };

  const changeOrder = (next: LinkOrder) => {
    setOrder(next);
    void load(0, { ...requested.current, order: next });
  };

  // R-UI-22: keep the tag and order and start again at page 0. Not paging, so no scroll or focus move.
  const changeUnread = (next: boolean) => {
    setUnread(next);
    void load(0, { ...requested.current, unread: next });
  };

  // R-UI-05, R-UI-20, R-UI-23: after a save, show page 0 with no filter, newest first and unread off.
  const onSaved = () => {
    setFilterInput('');
    setOrder('newest');
    setUnread(false);
    void load(0, DEFAULT_CONDITIONS);
  };

  // R-UI-26: the select cannot resend a value it already shows, so a failed list is reloaded from here.
  const retry = () => void load(requestedPage.current, requested.current);

  return (
    <main className="app" data-testid="app-root">
      <h1>linkshelf</h1>
      <LinkForm onSaved={onSaved} />
      <section className="browse" ref={browseTop}>
        <h2>저장한 링크</h2>
        <div className="list-controls">
          <TagFilter
            input={filterInput}
            onInputChange={setFilterInput}
            active={tag}
            error={filterError}
            busy={busy}
            onApply={applyFilter}
            onClear={clearFilter}
          />
          <SortSelect value={order} onChange={changeOrder} />
          <UnreadToggle checked={unread} onChange={changeUnread} />
        </div>
        <LinkList
          data={data}
          error={listError}
          busy={busy}
          onPage={(page) => void goToPage(page)}
          onRetry={retry}
        />
      </section>
    </main>
  );
}
