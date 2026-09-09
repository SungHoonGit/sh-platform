import { useEffect, useState } from "react";
import { apiDownload, apiDownloadShare, fileDownloadPath } from "../../api/client";
import type { PortfolioItem } from "../../types/resume";

export function period(start: string, end: string | null): string {
  return `${start ?? ""} ~ ${end ?? "현재"}`;
}

export const SECTION_LABELS: Record<string, string> = {
  careers: "경력",
  projects: "프로젝트",
  educations: "학력",
  skills: "스킬",
  certificates: "자격증",
  introductions: "자기소개",
  portfolioItems: "포트폴리오",
};

export const DEFAULT_ORDER = [
  "careers",
  "projects",
  "educations",
  "skills",
  "certificates",
  "introductions",
  "portfolioItems",
];

export const TEMPLATE_OPTIONS = ["CLASSIC", "MODERN", "SARAMIN"] as const;

export const TEMPLATE_LABELS: Record<string, string> = {
  CLASSIC: "클래식",
  MODERN: "모던",
  SARAMIN: "사람인형",
};

export function ProfilePhoto({
  photoUrl,
  className,
}: {
  photoUrl?: string | null;
  className: string;
}) {
  const [src, setSrc] = useState<string | null>(null);

  useEffect(() => {
    if (!photoUrl) return;
    const token = localStorage.getItem("accessToken");
    if (!token) return;
    let url: string | null = null;
    fetch(`/resume${photoUrl}`, { headers: { Authorization: `Bearer ${token}` } })
      .then((res) => (res.ok ? res.blob() : Promise.reject(new Error(String(res.status)))))
      .then((blob) => {
        url = URL.createObjectURL(blob);
        setSrc(url);
      })
      .catch(() => setSrc(null));
    return () => {
      if (url) URL.revokeObjectURL(url);
    };
  }, [photoUrl]);

  if (!src) return null;
  return <img src={src} alt="프로필 사진" className={className} />;
}

function extractFileId(path: string | null | undefined): number | null {
  const m = /\/files\/(\d+)\/download/.exec(path ?? "");
  return m ? Number(m[1]) : null;
}

/**
 * 포트폴리오 작업물 카드. (공유 뷰에선 shareToken으로 공개 파일 URL 사용)
 */
export function PortfolioCard({
  item,
  shareToken,
}: {
  item: PortfolioItem;
  shareToken?: string;
}) {
  const [thumbSrc, setThumbSrc] = useState<string | null>(null);
  const thumbFileId = extractFileId(item.thumbnailPath);

  useEffect(() => {
    if (!item.thumbnailPath || thumbFileId == null) return;
    if (shareToken) {
      setThumbSrc(`/resume/share/${shareToken}/files/${thumbFileId}`);
      return;
    }
    const token = localStorage.getItem("accessToken");
    if (!token) return;
    let url: string | null = null;
    fetch(`/resume${fileDownloadPath(item.thumbnailPath)}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => (res.ok ? res.blob() : Promise.reject(new Error(String(res.status)))))
      .then((blob) => {
        url = URL.createObjectURL(blob);
        setThumbSrc(url);
      })
      .catch(() => setThumbSrc(null));
    return () => {
      if (url) URL.revokeObjectURL(url);
    };
  }, [item.thumbnailPath, thumbFileId, shareToken]);

  const links = [
    item.githubUrl ? { label: "GitHub", href: item.githubUrl } : null,
    item.demoUrl ? { label: "데모", href: item.demoUrl } : null,
    item.videoUrl ? { label: "영상", href: item.videoUrl } : null,
    item.linkUrl ? { label: "링크", href: item.linkUrl } : null,
  ].filter((x): x is { label: string; href: string } => x !== null);

  const attachFileId = extractFileId(item.filePath);

  return (
    <div className="mb-2.5 border border-gray-200 rounded-lg overflow-hidden break-inside-avoid">
      {thumbSrc && (
        <img
          src={thumbSrc}
          alt={item.title}
          className="w-full max-h-44 object-cover"
        />
      )}
      <div className={`${thumbSrc ? "p-3" : "p-0 pt-1"}`}>
        <p className="font-semibold text-sm text-gray-800">{item.title}</p>
        {item.description && (
          <p className="mt-0.5 text-xs text-gray-600 whitespace-pre-wrap">{item.description}</p>
        )}
        {links.length > 0 && (
          <div className="mt-1.5 flex flex-wrap gap-x-2.5 gap-y-0.5">
            {links.map((l) => (
              <a
                key={l.label}
                href={l.href}
                target="_blank"
                rel="noreferrer"
                className="text-xs text-blue-600 underline"
              >
                {l.label}
              </a>
            ))}
          </div>
        )}
        {item.filePath && attachFileId != null && (
          <button
            onClick={() => {
              const run = shareToken
                ? apiDownloadShare(`/${shareToken}/files/${attachFileId}`, `${item.title}.download`)
                : apiDownload(fileDownloadPath(item.filePath!), `${item.title}.download`);
              void run.catch(() => alert("다운로드에 실패했습니다."));
            }}
            className="mt-1.5 text-xs text-blue-700 border border-blue-200 bg-blue-50 rounded px-2 py-0.5 hover:bg-blue-100"
          >
            첨부파일 다운로드
          </button>
        )}
      </div>
    </div>
  );
}
