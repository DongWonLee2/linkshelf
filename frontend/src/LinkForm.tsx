import { type FormEvent, useRef, useState } from 'react';
import { type FieldError, failureMessage, saveLink } from './api';

const FIELDS = ['url', 'memo', 'tags'] as const;

// Split on commas, trim each piece, drop empty pieces. Case-merging and length rules stay on the server (spec 001).
export function parseTags(raw: string): string[] {
  return raw
    .split(',')
    .map((t) => t.trim())
    .filter((t) => t.length > 0);
}

export function LinkForm({ onSaved }: { onSaved: () => void }) {
  const [url, setUrl] = useState('');
  const [memo, setMemo] = useState('');
  const [tags, setTags] = useState('');
  const [saving, setSaving] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<FieldError[]>([]);
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [duplicate, setDuplicate] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  // Guards against a second submit before the disabled state re-renders (R-UI-04).
  const inFlight = useRef(false);

  function onInput(setter: (v: string) => void) {
    return (value: string) => {
      setter(value);
      // The saved notice stays until the next input starts (clarify Q3).
      setSaved(false);
    };
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (inFlight.current) return;
    inFlight.current = true;
    setSaving(true);
    setFieldErrors([]);
    setGeneralError(null);
    setDuplicate(null);
    setSaved(false);

    const result = await saveLink({ url, memo: memo === '' ? null : memo, tags: parseTags(tags) });

    inFlight.current = false;
    setSaving(false);
    if (result.ok) {
      setUrl('');
      setMemo('');
      setTags('');
      setSaved(true);
      onSaved();
      return;
    }
    const error = result.error;
    if (error.kind === 'conflict') {
      setDuplicate(error.detail ?? '이미 저장된 링크입니다.');
    } else if (error.kind === 'validation' && error.errors.some((e) => FIELDS.includes(e.field as never))) {
      setFieldErrors(error.errors);
    } else {
      setGeneralError(failureMessage(error));
    }
  }

  function errorFor(field: (typeof FIELDS)[number]) {
    const messages = fieldErrors.filter((e) => e.field === field).map((e) => e.message);
    if (messages.length === 0) return null;
    return (
      <p className="field-error" data-testid="link-form-error" data-field={field} role="alert">
        {messages.join(', ')}
      </p>
    );
  }

  return (
    <form className="link-form" data-testid="link-form" onSubmit={handleSubmit} noValidate>
      <h2>링크 저장</h2>
      <label className="field">
        <span>URL</span>
        <input
          data-testid="link-url-input"
          type="text"
          value={url}
          onChange={(e) => onInput(setUrl)(e.target.value)}
          placeholder="https://"
          aria-invalid={fieldErrors.some((e) => e.field === 'url')}
        />
      </label>
      {errorFor('url')}
      <label className="field">
        <span>메모</span>
        <textarea
          data-testid="link-memo-input"
          value={memo}
          onChange={(e) => onInput(setMemo)(e.target.value)}
          rows={2}
          aria-invalid={fieldErrors.some((e) => e.field === 'memo')}
        />
      </label>
      {errorFor('memo')}
      <label className="field">
        <span>태그(쉼표로 구분)</span>
        <input
          data-testid="link-tags-input"
          type="text"
          value={tags}
          onChange={(e) => onInput(setTags)(e.target.value)}
          aria-invalid={fieldErrors.some((e) => e.field === 'tags')}
        />
      </label>
      {errorFor('tags')}
      <div className="form-actions">
        <button data-testid="link-save-button" type="submit" disabled={saving}>
          {saving ? '저장 중…' : '저장'}
        </button>
        {saved && (
          <span className="notice success" data-testid="link-form-saved" role="status">
            저장했습니다.
          </span>
        )}
      </div>
      {duplicate && (
        <p className="notice warning" data-testid="link-form-duplicate" role="alert">
          {duplicate}
        </p>
      )}
      {generalError && (
        <p className="notice error" role="alert">
          저장하지 못했습니다: {generalError}
        </p>
      )}
    </form>
  );
}
