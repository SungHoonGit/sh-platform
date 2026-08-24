import { useEffect, useState } from "react";

export function period(start: string, end: string | null): string {
  return `${start} ~ ${end ?? "현재"}`;
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
