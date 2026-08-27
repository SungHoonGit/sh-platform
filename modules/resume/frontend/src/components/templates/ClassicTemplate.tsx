import type { ResumeView } from "../../types/resume";
import { apiDownload, fileDownloadPath } from "../../api/client";
import { ProfilePhoto, period } from "./shared";

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mb-8 break-inside-avoid">
      <h2 className="text-lg font-bold border-b-2 border-gray-900 pb-1 mb-4">{title}</h2>
      {children}
    </section>
  );
}

export default function ClassicTemplate({
  view,
  order,
}: {
  view: ResumeView;
  order: string[];
}) {
  const p = view.profile;
  const nodes: Record<string, React.ReactNode> = {
    careers:
      view.careers.length > 0 ? (
        <Section title="경력">
          {view.careers.map((c) => (
            <article key={c.id} className="mb-5 last:mb-0">
              <div className="flex justify-between items-baseline">
                <h3 className="font-bold">{c.company}</h3>
                <span className="text-sm text-gray-500">{period(c.startDate, c.endDate)}</span>
              </div>
              <p className="text-sm text-gray-600">{c.title}</p>
              {c.description && (
                <p className="mt-1.5 whitespace-pre-wrap text-sm leading-relaxed">{c.description}</p>
              )}
            </article>
          ))}
        </Section>
      ) : null,
    projects:
      view.projects.length > 0 ? (
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
                    <span key={t.trim()} className="px-2 py-0.5 bg-gray-100 rounded text-xs">
                      {t.trim()}
                    </span>
                  ))}
                </p>
              )}
              {pr.description && (
                <p className="mt-1.5 whitespace-pre-wrap text-sm leading-relaxed">{pr.description}</p>
              )}
              {pr.linkUrl && (
                <a href={pr.linkUrl} target="_blank" rel="noreferrer" className="text-sm text-blue-600 underline">
                  {pr.linkUrl}
                </a>
              )}
            </article>
          ))}
        </Section>
      ) : null,
    educations:
      view.educations.length > 0 ? (
        <Section title="학력">
          {view.educations.map((ed) => (
            <article key={ed.id} className="mb-3 last:mb-0 flex justify-between items-baseline">
              <div>
                <span className="font-semibold">{ed.school}</span>
                {ed.schoolType && (
                  <span className="text-gray-500 text-sm"> {" "}· {ed.schoolType}</span>
                )}
                {ed.gpa && (
                  <span className="text-gray-500 text-sm"> {" "}· 학점 {ed.gpa}</span>
                )}
                {ed.major && (
                  <span className="text-gray-600">
                    {" "}
                    · {ed.major}
                    {ed.degree ? ` (${ed.degree})` : ""}
                  </span>
                )}
              </div>
              <span className="text-sm text-gray-500 shrink-0 ml-4">
                {period(ed.startDate, ed.endDate)}
                {ed.status ? ` · ${ed.status}` : ""}
              </span>
            </article>
          ))}
        </Section>
      ) : null,
    skills:
      view.skills.length > 0 ? (
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
      ) : null,
    certificates:
      view.certificates.length > 0 ? (
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
      ) : null,
    introductions:
      view.introductions.length > 0 ? (
        <Section title="자기소개">
          {view.introductions.map((it) => (
            <article key={it.id} className="mb-4 last:mb-0">
              <h3 className="font-semibold mb-1">{it.title}</h3>
              <p className="whitespace-pre-wrap text-sm leading-relaxed text-gray-800">{it.content}</p>
            </article>
          ))}
        </Section>
      ) : null,
    portfolioItems:
      view.portfolioItems.length > 0 ? (
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
                    className="font-semibold text-blue-600 underline print:text-gray-900 print:no-underline"
                  >
                    {pi.title}
                  </button>
                ) : pi.linkUrl ? (
                  <a
                    href={pi.linkUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="font-semibold text-blue-600 underline"
                  >
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
      ) : null,
  };

  return (
    <>
      <header className="mb-8 pb-6 border-b border-gray-300 print:pb-4">
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
          {p?.photoUrl && (
            <ProfilePhoto
              photoUrl={p.photoUrl}
              className="w-24 h-32 rounded-lg border border-gray-300 object-cover shrink-0 print:w-20 print:h-28"
            />
          )}
        </div>
      </header>
      {order.map((key) => nodes[key] ?? null)}
    </>
  );
}
