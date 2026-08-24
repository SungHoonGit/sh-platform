import type { ResumeView } from "../../types/resume";
import { ProfilePhoto, period } from "./shared";

function SideSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="mb-5 break-inside-avoid">
      <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 border-b border-slate-600 pb-1 mb-2.5">
        {title}
      </h2>
      {children}
    </div>
  );
}

function MainSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mb-7 break-inside-avoid">
      <h2 className="text-base font-bold text-slate-800 flex items-center gap-2 mb-3 print:text-sm">
        <span className="w-1 h-4 bg-teal-600 rounded print:h-3" />
        {title}
      </h2>
      {children}
    </section>
  );
}

export default function ModernTemplate({
  view,
  order,
}: {
  view: ResumeView;
  order: string[];
}) {
  const p = view.profile;
  const sideKeys = new Set(["educations", "skills", "certificates"]);
  const mainOrder = order.filter((k) => !sideKeys.has(k));
  const sideOrder = order.filter((k) => sideKeys.has(k));

  const mainNodes: Record<string, React.ReactNode> = {
    careers:
      view.careers.length > 0 ? (
        <MainSection title="경력">
          {view.careers.map((c) => (
            <article key={c.id} className="mb-4 last:mb-0 pl-3 border-l-2 border-gray-200">
              <div className="flex justify-between items-baseline gap-2">
                <h3 className="font-bold text-slate-800">{c.company}</h3>
                <span className="text-xs text-gray-500 shrink-0">{period(c.startDate, c.endDate)}</span>
              </div>
              {c.title && <p className="text-sm text-teal-700 font-medium">{c.title}</p>}
              {c.description && (
                <p className="mt-1 whitespace-pre-wrap text-sm leading-relaxed text-gray-600">{c.description}</p>
              )}
            </article>
          ))}
        </MainSection>
      ) : null,
    projects:
      view.projects.length > 0 ? (
        <MainSection title="프로젝트">
          {view.projects.map((pr) => (
            <article key={pr.id} className="mb-4 last:mb-0 pl-3 border-l-2 border-gray-200">
              <div className="flex justify-between items-baseline gap-2">
                <h3 className="font-bold text-slate-800">{pr.name}</h3>
                <span className="text-xs text-gray-500 shrink-0">{period(pr.startDate, pr.endDate)}</span>
              </div>
              {pr.role && <p className="text-sm text-teal-700 font-medium">{pr.role}</p>}
              {pr.techStack && (
                <p className="mt-1 flex flex-wrap gap-1">
                  {pr.techStack.split(",").map((t) => (
                    <span key={t.trim()} className="px-1.5 py-0.5 bg-teal-50 text-teal-800 rounded text-[11px]">
                      {t.trim()}
                    </span>
                  ))}
                </p>
              )}
              {pr.description && (
                <p className="mt-1 whitespace-pre-wrap text-sm leading-relaxed text-gray-600">{pr.description}</p>
              )}
              {pr.linkUrl && (
                <a href={pr.linkUrl} target="_blank" rel="noreferrer" className="text-xs text-blue-600 underline">
                  {pr.linkUrl}
                </a>
              )}
            </article>
          ))}
        </MainSection>
      ) : null,
    introductions:
      view.introductions.length > 0 ? (
        <MainSection title="자기소개">
          {view.introductions.map((it) => (
            <article key={it.id} className="mb-3 last:mb-0">
              <h3 className="font-semibold text-sm text-slate-800 mb-0.5">{it.title}</h3>
              <p className="whitespace-pre-wrap text-sm leading-relaxed text-gray-600">{it.content}</p>
            </article>
          ))}
        </MainSection>
      ) : null,
    portfolioItems:
      view.portfolioItems.length > 0 ? (
        <MainSection title="포트폴리오">
          <ul className="space-y-1.5">
            {view.portfolioItems.map((pi) => (
              <li key={pi.id} className="text-sm">
                <span className="font-semibold text-slate-800">{pi.title}</span>
                {pi.linkUrl && (
                  <a href={pi.linkUrl} target="_blank" rel="noreferrer" className="ml-2 text-xs text-blue-600 underline">
                    {pi.linkUrl}
                  </a>
                )}
                {pi.description && <p className="text-xs text-gray-500">{pi.description}</p>}
              </li>
            ))}
          </ul>
        </MainSection>
      ) : null,
  };

  const sideNodes: Record<string, React.ReactNode> = {
    educations:
      view.educations.length > 0 ? (
        <SideSection title="학력">
          {view.educations.map((ed) => (
            <div key={ed.id} className="mb-2 last:mb-0 text-xs">
              <p className="font-semibold text-white">{ed.school}</p>
              <p className="text-slate-300">
                {[ed.major, ed.degree].filter(Boolean).join(" · ")}
              </p>
              <p className="text-slate-400">
                {period(ed.startDate, ed.endDate)}
                {ed.status ? ` (${ed.status})` : ""}
              </p>
            </div>
          ))}
        </SideSection>
      ) : null,
    skills:
      view.skills.length > 0 ? (
        <SideSection title="스킬">
          <div className="flex flex-wrap gap-1.5">
            {view.skills.map((s) => (
              <span
                key={s.id}
                className="px-2 py-0.5 bg-slate-700 text-slate-100 rounded-full text-[11px] font-medium"
              >
                {s.name}
              </span>
            ))}
          </div>
        </SideSection>
      ) : null,
    certificates:
      view.certificates.length > 0 ? (
        <SideSection title="자격증">
          <ul className="space-y-1.5">
            {view.certificates.map((c) => (
              <li key={c.id} className="text-xs">
                <p className="font-semibold text-white">{c.name}</p>
                <p className="text-slate-400">
                  {c.issuer ? `${c.issuer} · ` : ""}
                  {c.acquiredAt}
                </p>
              </li>
            ))}
          </ul>
        </SideSection>
      ) : null,
  };

  return (
    <>
      <style>{`@media print { @page { margin: 0; } .modern-shell { box-shadow:none; } }`}</style>
      <div className="modern-shell flex bg-white shadow-sm max-w-3xl mx-auto print:max-w-none print:shadow-none">
        {/* 사이드바 */}
        <aside className="w-52 shrink-0 bg-slate-800 p-6 text-white print:w-44 print:p-4">
          {p?.photoUrl ? (
            <ProfilePhoto
              photoUrl={p.photoUrl}
              className="w-28 h-28 rounded-full object-cover mx-auto mb-5 border-4 border-slate-600 print:w-24 print:h-24"
            />
          ) : (
            <div className="w-28 h-28 rounded-full bg-slate-700 mx-auto mb-5 flex items-center justify-center text-3xl font-bold text-slate-400 print:w-24 print:h-24">
              {(p?.name ?? "?").charAt(0)}
            </div>
          )}
          <h1 className="text-xl font-bold text-center">{p?.name ?? "이름 미등록"}</h1>
          {p?.headline && (
            <p className="text-[11px] text-teal-300 text-center mt-1 mb-4 leading-snug">{p.headline}</p>
          )}

          <SideSection title="연락처">
            <ul className="space-y-1.5 text-[11px] text-slate-300 break-all">
              {p?.email && <li>✉ {p.email}</li>}
              {p?.phone && <li>☎ {p.phone}</li>}
              {p?.birthDate && <li>🎂 {p.birthDate}</li>}
              {p?.address && <li>📍 {p.address}</li>}
            </ul>
          </SideSection>

          {sideOrder.map((key) => sideNodes[key] ?? null)}
        </aside>

        {/* 본문 */}
        <main className="flex-1 p-8 print:p-6">
          {mainOrder.map((key) => mainNodes[key] ?? null)}
        </main>
      </div>
    </>
  );
}
