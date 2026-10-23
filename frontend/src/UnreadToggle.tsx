type Props = {
  checked: boolean;
  onChange: (unread: boolean) => void;
};

// Not disabled while loading, like SortSelect: Chromium drops focus from a disabled control (R-UI-21).
export function UnreadToggle({ checked, onChange }: Props) {
  return (
    <label className="field inline unread-toggle">
      <input
        data-testid="link-unread-only-toggle"
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
      />
      <span>안 읽은 것만</span>
    </label>
  );
}
