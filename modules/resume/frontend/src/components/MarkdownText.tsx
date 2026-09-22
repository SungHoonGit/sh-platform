import ReactMarkdown from "react-markdown";

/**
 * 마크다운 문자열을 .md-view 스타일로 렌더링한다 (raw HTML 비활성 기본값 — XSS 방지).
 * 편집 미리보기·이력서 템플릿·목록 상세 미리보기에서 공용으로 사용한다.
 */
export default function MarkdownText({ children, className }: { children: string; className?: string }) {
  if (!children?.trim()) return null;
  return (
    <div className={`md-view ${className ?? ""}`}>
      <ReactMarkdown>{children}</ReactMarkdown>
    </div>
  );
}
