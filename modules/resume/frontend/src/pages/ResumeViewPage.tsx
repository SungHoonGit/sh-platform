import { useEffect, useState } from "react";
import { apiDownload, apiGet, fileDownloadPath } from "../api/client";
import type { ResumeView } from "../types/resume";

function period(start: string, end: string | null): string {
  const e = end ?? "현재";
  return `${start} ~ ${e}`;
}

function ProfilePhoto({ photoUrl }: { photoUrl: string }) {
  const [src, setSrc] = useState<string | null>(null);

  useEffect(() => {
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
  return (
    <img
      src={src}
      alt="프로필 사진"
      className="w-24 h-32 rounded-lg border border-gray-300 object-cover shrink-0 print:w-20 print:h-28"
    />
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mb-8 break-inside-avoid">
      <h2 className="text-lg font-bold border-b-2 border-gray-900 pb-1 mb-4">{title}</h2>
      {children}
    </section>
  );
}

export default function ResumeViewPage() {
  const [view, setView] = useState<ResumeView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiGet<ResumeView>("/view")
      .then(setView)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <div className="p-10 text-center text-gray-500">불러오는 중...</div>;
  }

  if (error === "UNAUTHORIZED") {
    return (
      <div className="p-10 text-center">
        <p className="mb-4">로그인이 필요합니다.</p>
        <a
          href={`/?redirect=${encodeURIComponent("/resume/")}`}
          className="px-4 py-2 bg-gray-900 text-white rounded hover:bg-gray-700"
        >
          로그인하러 가기
        </a>
      </div>
    );
  }

  if (error || !view) {
    return <div className="p-10 text-center text-red-500">이력서를 불러오지 못했습니다. ({error})</div>;
  }

  const p = view.profile;

  return (
    <div className="min-h-screen bg-gray-100 py-6 print:bg-white print:py-0">
      <style>{`@media print { @page { margin: 15mm; } }`}</style>

      <div className="max-w-3xl mx-auto bg-white shadow-sm px-10 py-8 print:shadow-none print:px-0">
        <div className="flex justify-end gap-2 mb-4 print:hidden">
          <a
            href="#edit"
            className="px-3 py-1.5 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50"
          >
            편집
          </a>
          <button
            onClick={() => window.print()}
            className="px-3 py-1.5 text-sm bg-gray-900 text-white rounded hover:bg-gray-700"
          >
            PDF로 인쇄
          </button>
        </div>

        {/* 인적사항 */}
        <header className="mb-8 pb-6 border-b border-gray-300">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h1 className="text-3xl font-bold">{p?.name ?? "이름 미등록"}</h1>
              {p?.headline && <p className="mt-1 text-gray-600">{p.headline}</p>}
              <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-sm text-gray-500">
                {p?.email && <span>{p.email}</span>}
                {p?.phone && <span>{p.phone}</span>}
                {p?.address && <span>{p.address}</span>}
                {p?.birthDate && <span>{p.birthDate}</span>}
              </div>
            </div>
            {p?.photoUrl && <ProfilePhoto photoUrl={p.photoUrl} />}
          </div>
        </header>

        {view.careers.length > 0 && (
          <Section title="경력">
            {view.careers.map((c) => (
              <article key={c.id} className="mb-5 last:mb-0">
                <div className="flex justify-between items-baseline">
                  <h3 className="font-bold">{c.company}</h3>
                  <span className="text-sm text-gray-500">{period(c.startDate, c.endDate)}</span>
                </div>
                <p className="text-sm text-gray-600">{c.title}</p>
                {c.description && <p className="mt-1.5 whitespace-pre-wrap text-sm leading-relaxed">{c.description}</p>}
              </article>
            ))}
          </Section>
        )}

        {view.projects.length > 0 && (
          <Section title="프로젝트">
            {view.projects.map((pr) => (
              <article key={pr.id} className="mb-5 last:mb-0">
                <div className="flex justify-between items-baseline">
                  <h3 className="font-bold">{pr.name}</h3>
                  <span className="text-sm text-gray-500">{period(pr.startDate, pr.endDate)}</span>
                </div>
                {pr.role && <p className="text-sm text-gray-600">{pr.role}</p>}
                {pr.techStack && (
                  <p className="mt-1 flex flex-wrap gap-1">
                    {pr.techStack.split(",").map((t) => (
                      <span key={t.trim()} className="px-2 py-0.5 bg-gray-100 rounded text-xs">{t.trim()}</span>
                    ))}
                  </p>
                )}
                {pr.description && <p className="mt-1.5 whitespace-pre-wrap text-sm leading-relaxed">{pr.description}</p>}
                {pr.linkUrl && (
                  <a href={pr.linkUrl} target="_blank" rel="noreferrer" className="text-sm text-blue-600 underline">
                    {pr.linkUrl}
                  </a>
                )}
              </article>
            ))}
          </Section>
        )}

        {view.educations.length > 0 && (
          <Section title="학력">
            {view.educations.map((ed) => (
              <article key={ed.id} className="mb-3 last:mb-0 flex justify-between items-baseline">
                <div>
                  <span className="font-semibold">{ed.school}</span>
                  {ed.major && <span className="text-gray-600"> · {ed.major}{ed.degree ? ` (${ed.degree})` : ""}</span>}
                </div>
                <span className="text-sm text-gray-500 shrink-0 ml-4">
                  {period(ed.startDate, ed.endDate)}{ed.status ? ` · ${ed.status}` : ""}
                </span>
              </article>
            ))}
          </Section>
        )}

        {view.skills.length > 0 && (
          <Section title="스킬">
            <div className="flex flex-wrap gap-2">
              {view.skills.map((s) => (
                <span key={s.id} className="px-2.5 py-1 bg-gray-100 rounded-full text-sm">
                  {s.name}
                  {s.level ? ` · ${s.level}` : ""}
                </span>
              ))}
            </div>
          </Section>
        )}

        {view.certificates.length > 0 && (
          <Section title="자격증">
            <ul className="space-y-1.5">
              {view.certificates.map((c) => (
                <li key={c.id} className="flex justify-between items-baseline">
                  <span>
                    <span className="font-semibold">{c.name}</span>
                    {c.issuer && <span className="text-gray-500 text-sm"> ({c.issuer})</span>}
                  </span>
                  <span className="text-sm text-gray-500 shrink-0 ml-4">{c.acquiredAt}</span>
                </li>
              ))}
            </ul>
          </Section>
        )}

        {view.introductions.length > 0 && (
          <Section title="자기소개">
            {view.introductions.map((it) => (
              <article key={it.id} className="mb-4 last:mb-0">
                <h3 className="font-semibold mb-1">{it.title}</h3>
                <p className="whitespace-pre-wrap text-sm leading-relaxed text-gray-800">{it.content}</p>
              </article>
            ))}
          </Section>
        )}

        {view.portfolioItems.length > 0 && (
          <Section title="포트폴리오">
            <ul className="space-y-2">
              {view.portfolioItems.map((pi) => (
                <li key={pi.id}>
                  {pi.itemType === "FILE" && pi.filePath ? (
                    <button
                      onClick={() =>
                        void apiDownload(fileDownloadPath(pi.filePath!), `${pi.title}`).catch(() =>
                          alert("다운로드에 실패했습니다."),
                        )
                      }
                      className="font-semibold text-blue-600 underline"
                    >
                      {pi.title}
                    </button>
                  ) : pi.linkUrl ? (
                    <a href={pi.linkUrl} target="_blank" rel="noreferrer" className="font-semibold text-blue-600 underline">
                      {pi.title}
                    </a>
                  ) : (
                    <span className="font-semibold">{pi.title}</span>
                  )}
                  {pi.description && <p className="text-sm text-gray-600">{pi.description}</p>}
                </li>
              ))}
            </ul>
          </Section>
        )}
      </div>
    </div>
  );
}
