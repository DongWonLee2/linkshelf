import { useCallback, useEffect, useRef, useState } from 'react';
import { type LinkOrder, type LinkPage, failureMessage, listLinks } from './api';
import { LinkForm } from './LinkForm';
import { LinkList } from './LinkList';
import { SortSelect } from './SortSelect';
import { TagFilter } from './TagFilter';
import './styles.css';

export function App() {
  const [data, setData] = useState<LinkPage | null>(null);
  const [tag, setTag] = useState<string | null>(null);
  const [filterInput, setFilterInput] = useState('');
  const [order, setOrder] = useState<LinkOrder>('newest');
  const [listError, setListError] = useState<string | null>(null);
  const [filterError, setFilterError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  // Only the newest request may update the screen.
  const latest = useRef(0);
  // An order change must keep a filter or clear that is still loading, so it reads the newest requested tag.
  const requestedTag = useRef<string | null>(null);
  const shownTag = useRef<string | null>(null);
  const browseTop = useRef<HTMLElement>(null);

  // Resolves true when this request's page was shown.
  const load = useCallback(
    async (page: number, nextTag: string | null, nextOrder: LinkOrder, fromFilter: boolean) => {
      const mine = ++latest.current;
      requestedTag.current = nextTag;
      setBusy(true);
      const result = await listLinks({ page, tag: nextTag, order: nextOrder });
      if (mine !== latest.current) return false;
      setBusy(false);

      if (result.ok) {
        setData(result.data);
        shownTag.current = nextTag;
        setTag(nextTag);
        setListError(null);
        setFilterError(null);
        return true;
      }
      requestedTag.current = shownTag.current;
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

  const changeOrder = (next: LinkOrder) => {
    setOrder(next);
    void load(0, requestedTag.current, next, false);
  };

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
