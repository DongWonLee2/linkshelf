import { useCallback, useEffect, useRef, useState } from 'react';
import { type LinkOrder, type LinkPage, failureMessage, listLinks } from './api';
import { LinkForm } from './LinkForm';
import { LinkList } from './LinkList';
import { SortSelect } from './SortSelect';
import { TagFilter } from './TagFilter';
import './styles.css';

// Screen state: current page, applied tag and order. A reload starts again at page 0, no filter, newest first
// (spec 003, spec 004 R-UI-18).
export function App() {
  const [data, setData] = useState<LinkPage | null>(null);
  const [tag, setTag] = useState<string | null>(null);
  const [filterInput, setFilterInput] = useState('');
  // The user's choice shows at once, even if loading that order then fails (spec 004 edge cases).
  const [order, setOrder] = useState<LinkOrder>('newest');
  const [listError, setListError] = useState<string | null>(null);
  const [filterError, setFilterError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  // Only the newest request may update the screen.
  const latest = useRef(0);
  const browseTop = useRef<HTMLElement>(null);

  // Resolves true when this request's page was shown.
  const load = useCallback(
    async (page: number, nextTag: string | null, nextOrder: LinkOrder, fromFilter: boolean) => {
      const mine = ++latest.current;
      setBusy(true);
      const result = await listLinks({ page, tag: nextTag, order: nextOrder });
      if (mine !== latest.current) return false;
      setBusy(false);

      if (result.ok) {
        setData(result.data);
        setTag(nextTag);
        setListError(null);
        setFilterError(null);
        return true;
      }
      const error = result.error;
      if (fromFilter && error.kind === 'validation') {
        // R-UI-08: keep the previous list and filter, show the error by the filter input.
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
    if (await load(page, tag, order, false)) pagedTo.current = true;
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
    void load(0, null, 'newest', false);
  }, [load]);

  const applyFilter = (input: string) => void load(0, input.trim(), order, true);

  const clearFilter = () => {
    setFilterInput('');
    void load(0, null, order, true);
  };

  // R-UI-19: keep the filter and start again at page 0. Not paging, so no scroll or focus move.
  const changeOrder = (next: LinkOrder) => {
    setOrder(next);
    void load(0, tag, next, false);
  };

  // R-UI-05, R-UI-20: after a save, drop the filter and show page 0 newest first so the new link is on top.
  const onSaved = () => {
    setFilterInput('');
    setOrder('newest');
    void load(0, null, 'newest', false);
  };

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
        </div>
        <LinkList data={data} error={listError} busy={busy} onPage={(page) => void goToPage(page)} />
      </section>
    </main>
  );
}
