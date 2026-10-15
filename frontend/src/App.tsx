import { useCallback, useEffect, useRef, useState } from 'react';
import { type LinkPage, failureMessage, listLinks } from './api';
import { LinkForm } from './LinkForm';
import { LinkList } from './LinkList';
import { TagFilter } from './TagFilter';
import './styles.css';

// Screen state: current page and applied tag. A reload starts again at page 0 with no filter (spec 003).
export function App() {
  const [data, setData] = useState<LinkPage | null>(null);
  const [tag, setTag] = useState<string | null>(null);
  const [filterInput, setFilterInput] = useState('');
  const [listError, setListError] = useState<string | null>(null);
  const [filterError, setFilterError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  // Only the newest request may update the screen.
  const latest = useRef(0);

  const load = useCallback(async (page: number, nextTag: string | null, fromFilter: boolean) => {
    const mine = ++latest.current;
    setBusy(true);
    const result = await listLinks({ page, tag: nextTag });
    if (mine !== latest.current) return;
    setBusy(false);

    if (result.ok) {
      setData(result.data);
      setTag(nextTag);
      setListError(null);
      setFilterError(null);
      return;
    }
    const error = result.error;
    if (fromFilter && error.kind === 'validation') {
      // R-UI-08: keep the previous list and filter, show the error by the filter input.
      const tagErrors = error.errors.filter((e) => e.field === 'tag').map((e) => e.message);
      setFilterError(tagErrors.length > 0 ? tagErrors.join(', ') : failureMessage(error));
      return;
    }
    setListError(failureMessage(error));
  }, []);

  useEffect(() => {
    void load(0, null, false);
  }, [load]);

  const applyFilter = (input: string) => void load(0, input.trim(), true);

  const clearFilter = () => {
    setFilterInput('');
    void load(0, null, true);
  };

  // R-UI-05: after a save, drop the filter and show page 0 so the new link is on top.
  const onSaved = () => {
    setFilterInput('');
    void load(0, null, false);
  };

  return (
    <main className="app" data-testid="app-root">
      <h1>linkshelf</h1>
      <LinkForm onSaved={onSaved} />
      <section className="browse">
        <h2>저장한 링크</h2>
        <TagFilter
          input={filterInput}
          onInputChange={setFilterInput}
          active={tag}
          error={filterError}
          busy={busy}
          onApply={applyFilter}
          onClear={clearFilter}
        />
        <LinkList data={data} error={listError} busy={busy} onPage={(page) => void load(page, tag, false)} />
      </section>
    </main>
  );
}
