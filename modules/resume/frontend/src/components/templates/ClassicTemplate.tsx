import type { ResumeView } from "../../types/resume";
import { FileThumb, period, PortfolioCard, ProfilePhoto, projectLinks, ymd } from "./shared";

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mb-5 break-inside-avoid">
      <h2 className="text-lg font-bold border-b border-gray-900 pb-0.5 mb-2.5 inline-block">{title}</h2>
      {children}
    </section>
  );
}

export default function ClassicTemplate({
  view,
  order,
  shareToken,
}: {
  view: ResumeView;
  order: string[];
  shareToken?: string;
}) {
  const p = view.profile;
  const nodes: Record<string, React.ReactNode> = {
    careers:
      view.careers.length > 0 ? (
        <Section title="경력">
          {view.careers.map((c) => (
              <article key={c.id} className="mb-3 last:mb-0">
              <div className="flex justify-between items-baseline">
                <h3 className="font-bold">{c.company}</h3>
                <span className="text-sm text-gray-500">{period(c.startDate, c.endDate)}</span>
              </div>
              <p className="text-sm text-gray-600">{c.title}</p>
              {c.description && (
                <p className="mt-1 whitespace-pre-wrap text-sm leading-normal">{c.description}</p>
              )}
              {c.items.length > 0 && (
                <ul className="mt-1.5 space-y-1">
                  {c.items.map((item) => (
                    <li key={item.id} className="text-sm leading-normal">
                      <span className="text-gray-500 text-xs">
                        {item.startDate ? `${ymd(item.startDate)} ~ ${item.endDate ? ymd(item.endDate) : "현재"} · ` : ""}
                      </span>
                      <span className="font-medium">{item.title}</span>
                      {item.description && (
                        <span className="text-gray-700 whitespace-pre-wrap"> — {item.description}</span>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </article>
          ))}
        </Section>
      ) : null,
    projects:
      view.projects.length > 0 ? (
        <Section title="프로젝트">
          {view.projects.map((pr) => (
              <article key={pr.id} className="mb-3 last:mb-0">
              {pr.thumbnailPath && (
                <FileThumb path={pr.thumbnailPath} className="w-full max-h-44 object-cover mb-2 rounded border border-gray-200" />
              )}
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
                <p className="mt-1 whitespace-pre-wrap text-sm leading-normal">{pr.description}</p>
              )}
              {projectLinks(pr).length > 0 && (
                <p className="mt-1.5 flex flex-wrap gap-x-2.5 gap-y-0.5">
                  {projectLinks(pr).map((l) => (
                    <a key={l.label} href={l.href} target="_blank" rel="noreferrer" className="text-sm text-blue-600 underline">
                      {l.label}
                    </a>
                  ))}
                </p>
              )}
            </article>
          ))}
        </Section>
      ) : null,
    educations:
      view.educations.length > 0 ? (
        <Section title="학력">
          {view.educations.map((ed) => (
              <article key={ed.id} className="mb-2 last:mb-0 flex justify-between items-baseline">
              <div>
                <span className="font-semibold">{ed.school}</span>
                {ed.schoolType && !ed.school.endsWith(ed.schoolType) && (
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
              <article key={it.id} className="mb-3 last:mb-0">
              <h3 className="font-semibold mb-0.5">{it.title}</h3>
              <p className="whitespace-pre-wrap text-sm leading-normal text-gray-800">{it.content}</p>
            </article>
          ))}
        </Section>
      ) : null,
    portfolioItems:
      view.portfolioItems.length > 0 ? (
        <Section title="포트폴리오">
          {view.portfolioItems.map((pi) => (
            <PortfolioCard key={pi.id} item={pi} shareToken={shareToken} />
          ))}
        </Section>
      ) : null,
  };

  return (
    <>
      <header className="mb-4 pb-3 border-b border-gray-300 print:pb-4">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold">{p?.name ?? "이름 미등록"}</h1>
            {p?.headline && <p className="mt-1 text-gray-600">{p.headline}</p>}
            <div className="mt-3 flex flex-wrap gap-x-3 gap-y-1 text-sm text-gray-500">
              {p?.email && <span className="whitespace-nowrap">{p.email}</span>}
              {p?.phone && <span className="whitespace-nowrap">{p.phone}</span>}
              {p?.address && <span className="whitespace-nowrap">{p.address}</span>}
              {p?.birthDate && <span className="whitespace-nowrap">{p.birthDate}</span>}
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
