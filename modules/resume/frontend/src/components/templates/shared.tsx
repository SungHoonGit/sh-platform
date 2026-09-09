import { useEffect, useState } from "react";
import { apiDownload, apiDownloadShare, fileDownloadPath } from "../../api/client";
import type { PortfolioItem } from "../../types/resume";

export function period(start: string, end: string | null): string {
  return `${start ?? ""} ~ ${end ?? "현재"}`;
}

/** 날짜 문자열을 "YYYY.MM.DD" 형태로 변환하고, 유효하지 않거나 비었으면 그대로 둔다. */
export function ymd(value: string | null | undefined): string {
  if (!value) return value ?? "";
  return value.replace(/-/g, ".");
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
 * 저장소 파일(썸네일 등)을 인증/공유 토큰으로 불러와 표시하는 이미지 컴포넌트.
 */
export function FileThumb({
  path,
  shareToken,
  className,
}: {
  path: string | null | undefined;
  shareToken?: string;
  className?: string;
}) {
  const [src, setSrc] = useState<string | null>(null);
  const fileId = extractFileId(path);

  useEffect(() => {
    if (!path || fileId == null) {
      setSrc(null);
      return;
    }
    if (shareToken) {
      setSrc(`/resume/share/${shareToken}/files/${fileId}`);
      return;
    }
    const token = localStorage.getItem("accessToken");
    if (!token) {
      setSrc(null);
      return;
    }
    let url: string | null = null;
    fetch(`/resume${fileDownloadPath(path)}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => (res.ok ? res.blob() : Promise.reject(new Error(String(res.status)))))
      .then((blob) => {
        url = URL.createObjectURL(blob);
        setSrc(url);
      })
      .catch(() => setSrc(null));
    return () => {
      if (url) URL.revokeObjectURL(url);
    };
  }, [path, fileId, shareToken]);

  if (!src) return null;
  return <img src={src} alt="썸네일" className={className} />;
}

/**
 * 이력서에 표시할 프로젝트/작업물 링크 목록.
 */
export function projectLinks(item: {
  githubUrl?: string | null;
  demoUrl?: string | null;
  videoUrl?: string | null;
  linkUrl?: string | null;
}) {
  return [
    item.githubUrl ? { label: "GitHub", href: item.githubUrl } : null,
    item.demoUrl ? { label: "데모", href: item.demoUrl } : null,
    item.videoUrl ? { label: "영상", href: item.videoUrl } : null,
    item.linkUrl ? { label: "링크", href: item.linkUrl } : null,
  ].filter((x): x is { label: string; href: string } => x !== null);
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
  const links = projectLinks(item);
  const attachFileId = extractFileId(item.filePath);

  return (
    <div className="mb-2.5 border border-gray-200 rounded-lg overflow-hidden break-inside-avoid">
      <FileThumb
        path={item.thumbnailPath}
        shareToken={shareToken}
        className="w-full max-h-44 object-cover"
      />
      <div className="p-3">
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
