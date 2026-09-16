import { Star } from "lucide-react";

/** 내 별점 표시 (1~5). null이면 미지정 텍스트. */
export default function Stars({ value, size = 14 }: { value: number | null; size?: number }) {
  if (value == null) return <span className="text-xs text-gray-400">미지정</span>;
  return (
    <span className="inline-flex items-center gap-0.5" title={`${value}/5`}>
      {[1, 2, 3, 4, 5].map((i) => (
        <Star
          key={i}
          size={size}
          className={i <= value ? "fill-amber-400 text-amber-400" : "text-gray-300"}
        />
      ))}
    </span>
  );
}
