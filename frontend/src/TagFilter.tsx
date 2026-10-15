import { type FormEvent } from 'react';

type Props = {
  input: string;
  onInputChange: (value: string) => void;
  active: string | null;
  error: string | null;
  busy: boolean;
  onApply: (tag: string) => void;
  onClear: () => void;
};

export function TagFilter({ input, onInputChange, active, error, busy, onApply, onClear }: Props) {
  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    // Blank or whitespace-only input means "clear the filter" (R-UI-07).
    if (input.trim() === '') onClear();
    else onApply(input);
  }

  return (
    <form className="tag-filter" onSubmit={handleSubmit} noValidate>
      <label className="field inline">
        <span>태그로 거르기</span>
        <input
          data-testid="tag-filter-input"
          type="text"
          value={input}
          onChange={(e) => onInputChange(e.target.value)}
          aria-invalid={error !== null}
        />
      </label>
      <button data-testid="tag-filter-apply" type="submit" disabled={busy}>
        적용
      </button>
      {active !== null && (
        <>
          <span className="active-filter" data-testid="tag-filter-active">
            현재 필터: {active}
          </span>
          <button data-testid="tag-filter-clear" type="button" onClick={onClear} disabled={busy}>
            해제
          </button>
        </>
      )}
      {error && (
        <p className="field-error" data-testid="tag-filter-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}
