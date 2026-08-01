export const REGIONS = [
  "서울", "경기", "인천", "부산", "대구", "대전", "광주", "울산", "세종",
  "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주",
];

export const DEFAULT_LOCATIONS = ["서울", "경기", "인천", "부산", "대구", "대전", "광주", "울산", "세종"];

export const CAREER_TOTAL = 15;

/** 경력 슬라이더의 전체 범위(0~15년) 여부를 판단한다. 전체 범위면 필터 미적용. */
export function isCareerActive(min: number, max: number): boolean {
  return min > 0 || max < CAREER_TOTAL;
}

export function CareerRangeSlider({
  min,
  max,
  onMinChange,
  onMaxChange,
}: {
  min: number;
  max: number;
  onMinChange: (v: number) => void;
  onMaxChange: (v: number) => void;
}) {
  const pct = (v: number) => (v / CAREER_TOTAL) * 100;
  return (
    <div>
      <div className="relative h-1.5 bg-slate-200 rounded-full mt-2">
        <div
          className="absolute h-1.5 bg-blue-500 rounded-full"
          style={{ left: `${pct(min)}%`, width: `${pct(max) - pct(min)}%` }}
        />
      </div>
      <div className="range-slider">
        <input
          type="range"
          min={0}
          max={CAREER_TOTAL}
          value={min}
          onChange={(e) => onMinChange(Math.min(Number(e.target.value), max))}
        />
        <input
          type="range"
          min={0}
          max={CAREER_TOTAL}
          value={max}
          onChange={(e) => onMaxChange(Math.max(Number(e.target.value), min))}
        />
      </div>
      <div className="flex justify-between text-[10px] text-slate-400 mt-0.5">
        <span>{min === 0 ? "신입" : `${min}년`}</span>
        <span>{max >= CAREER_TOTAL ? "15년+" : `${max}년`}</span>
      </div>
    </div>
  );
}

export function LocationMultiSelect({
  selected,
  onToggle,
  onSelectAll,
  onClear,
}: {
  selected: string[];
  onToggle: (loc: string) => void;
  onSelectAll: () => void;
  onClear: () => void;
}) {
  return (
    <div>
      <div className="flex items-center justify-between mb-1.5">
        <label className="text-[11px] font-medium text-slate-600">지역</label>
        <div className="flex gap-2">
          <button
            onClick={onSelectAll}
            className="text-[10px] text-blue-600 hover:text-blue-800"
          >
            전체선택
          </button>
          <button
            onClick={onClear}
            className="text-[10px] text-blue-600 hover:text-blue-800"
          >
            초기화
          </button>
        </div>
      </div>
      <div className="grid grid-cols-3 gap-1.5">
        {REGIONS.map((loc) => (
          <label
            key={loc}
            className={`flex items-center gap-1 px-2 py-1.5 rounded border cursor-pointer text-xs transition-colors ${
              selected.includes(loc)
                ? "border-blue-300 bg-blue-50 text-blue-700"
                : "border-slate-200 text-slate-600 hover:border-slate-300"
            }`}
          >
            <input
              type="checkbox"
              checked={selected.includes(loc)}
              onChange={() => onToggle(loc)}
              className="w-3 h-3 text-blue-600"
            />
            <span className="truncate">{loc}</span>
          </label>
        ))}
      </div>
    </div>
  );
}
