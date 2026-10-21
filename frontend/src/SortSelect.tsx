import { type LinkOrder } from './api';

type Props = {
  value: LinkOrder;
  onChange: (order: LinkOrder) => void;
};

// Not disabled while loading: Chromium drops focus from a disabled control, which R-UI-19 forbids.
// A change during loading is safe because only the newest request updates the screen.
export function SortSelect({ value, onChange }: Props) {
  return (
    <label className="field inline sort-select">
      <span>정렬</span>
      <select data-testid="link-sort-select" value={value} onChange={(e) => onChange(e.target.value as LinkOrder)}>
        <option value="newest">최신순</option>
        <option value="oldest">오래된순</option>
      </select>
    </label>
  );
}
