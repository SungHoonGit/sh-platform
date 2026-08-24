import { Fragment } from "react";
import type { ResumeView } from "../../types/resume";
import { ProfilePhoto, period } from "./shared";

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mb-6 break-inside-avoid border border-gray-300 rounded-sm">
      <h2 className="bg-slate-100 px-4 py-2 text-sm font-bold text-slate-800 border-b border-gray-300">
        ■ {title}
      </h2>
      <div className="px-4 py-3">{children}</div>
    </section>
  );
}

export default function SaraminTemplate({
  view,
  order,
}: {
  view: ResumeView;
  order: string[];
}) {
  const p = view.profile;
  const contacts: [string, string | null][] = [
    ["이메일", p?.email ?? null],
    ["전화번호", p?.phone ?? null],
    ["생년월일", p?.birthDate ?? null],
    ["주소", p?.address ?? null],
  ];

  const nodes: Record<string, React.ReactNode> = {
    careers:
      view.careers.length > 0 ? (
        <Section title="경력">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-gray-500 text-left">
                <th className="py-1 font-medium">회사명</th>
                <th className="py-1 font-medium">직무</th>
                <th className="py-1 font-medium">기간</th>
              </tr>
            </thead>
            <tbody>
              {view.careers.map((c) => (
                <tr key={c.id} className="border-t border-gray-100 align-top">
                  <td className="py-2 pr-3 font-semibold">{c.company}</td>
                  <td className="py-2 pr-3 text-gray-600">{c.title}</td>
                  <td className="py-2 whitespace-nowrap text-gray-500 text-xs">
                    {period(c.startDate, c.endDate)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {view.careers.some((c) => c.description) && (
            <ul className="mt-1 space-y-1.5">
              {view.careers
                .filter((c) => c.description)
                .map((c) => (
                  <li key={c.id} className="text-xs leading-relaxed text-gray-700 whitespace-pre-wrap">
                    ▸ [{c.company}] {c.description}
                  </li>
                ))}
            </ul>
          )}
        </Section>
      ) : null,
    projects:
      view.projects.length > 0 ? (
        <Section title="프로젝트">
          {view.projects.map((pr) => (
            <article key={pr.id} className="mb-3 last:mb-0 pb-3 border-b border-dashed border-gray-200 last:border-0">
              <div className="flex justify-between items-baseline">
                <h3 className="font-semibold text-sm">
                  {pr.name}
                  {pr.role && <span className="ml-2 text-xs font-normal text-blue-700">{pr.role}</span>}
                </h3>
                <span className="text-xs text-gray-500 shrink-0 ml-3">{period(pr.startDate, pr.endDate)}</span>
              </div>
              {pr.techStack && (
                <p className="mt-0.5 text-[11px] text-gray-500">사용기술: {pr.techStack}</p>
              )}
              {pr.description && (
                <p className="mt-1 text-xs leading-relaxed text-gray-700 whitespace-pre-wrap">{pr.description}</p>
              )}
              {pr.linkUrl && (
                <a href={pr.linkUrl} target="_blank" rel="noreferrer" className="text-xs text-blue-600 underline">
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
          <table className="w-full text-sm">
            <tbody>
              {view.educations.map((ed) => (
                <tr key={ed.id} className="border-t border-gray-100 first:border-0">
                  <td className="py-1.5 w-40 whitespace-nowrap text-xs text-gray-500">
                    {period(ed.startDate, ed.endDate)}
                  </td>
                  <td className="py-1.5">
                    <span className="font-semibold">{ed.school}</span>
                    <span className="text-gray-600 text-xs">
                      {[ed.major, ed.degree].filter(Boolean).join(" · ")}
                      {ed.status ? ` (${ed.status})` : ""}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Section>
      ) : null,
    skills:
      view.skills.length > 0 ? (
        <Section title="보유 스킬">
          <table className="w-full text-sm">
            <tbody>
              {view.skills.map((s) => (
                <tr key={s.id} className="border-t border-gray-100 first:border-0">
                  <td className="py-1.5 font-semibold w-32">{s.name}</td>
                  <td className="py-1.5 text-gray-600 text-xs">
                    {[s.level, s.category].filter(Boolean).join(" / ") || "-"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Section>
      ) : null,
    certificates:
      view.certificates.length > 0 ? (
        <Section title="자격증">
          <ul className="space-y-1 text-sm">
            {view.certificates.map((c) => (
              <li key={c.id} className="flex justify-between">
                <span>
                  <span className="font-semibold">{c.name}</span>
                  {c.issuer && <span className="text-gray-500 text-xs ml-2">{c.issuer}</span>}
                </span>
                <span className="text-xs text-gray-500 shrink-0 ml-3">{c.acquiredAt}</span>
              </li>
            ))}
          </ul>
        </Section>
      ) : null,
    introductions:
      view.introductions.length > 0 ? (
        <Section title="자기소개">
          {view.introductions.map((it) => (
            <article key={it.id} className="mb-3 last:mb-0 pb-3 border-b border-dashed border-gray-200 last:border-0 last:pb-0">
              <h3 className="font-semibold text-sm mb-1">{it.title}</h3>
              <p className="whitespace-pre-wrap text-xs leading-relaxed text-gray-700">{it.content}</p>
            </article>
          ))}
        </Section>
      ) : null,
    portfolioItems:
      view.portfolioItems.length > 0 ? (
        <Section title="포트폴리오">
          <ul className="space-y-1.5 text-sm">
            {view.portfolioItems.map((pi) => (
              <li key={pi.id}>
                <span className={`inline-block mr-2 px-1.5 py-0.5 text-[10px] rounded ${
                  pi.itemType === "FILE" ? "bg-amber-100 text-amber-800" : "bg-blue-100 text-blue-800"
                }`}>
                  {pi.itemType}
                </span>
                <span className="font-semibold">{pi.title}</span>
                {pi.linkUrl && (
                  <a href={pi.linkUrl} target="_blank" rel="noreferrer" className="ml-2 text-xs text-blue-600 underline">
                    바로가기
                  </a>
                )}
                {pi.description && <p className="text-xs text-gray-600 mt-0.5">{pi.description}</p>}
              </li>
            ))}
          </ul>
        </Section>
      ) : null,
  };

  return (
    <>
      {/* 상단 프로필 박스 */}
      <header className="mb-6 border-2 border-slate-800 rounded-sm p-5 flex gap-5 print:p-4 print:mb-4">
        {p?.photoUrl && (
          <ProfilePhoto
            photoUrl={p.photoUrl}
            className="w-[100px] h-[140px] border border-gray-300 object-cover shrink-0"
          />
        )}
        <div className="min-w-0 flex-1">
          <h1 className="text-2xl font-bold tracking-wide">{p?.name ?? "이름 미등록"}</h1>
          {p?.headline && <p className="text-sm text-slate-600 mt-0.5">{p.headline}</p>}
          <table className="mt-3 text-xs w-full max-w-md">
            <tbody>
              {contacts
                .reduce<[string, string][][]>((rows, item, i) => {
                  if (i % 2 === 0) rows.push([]);
                  rows[rows.length - 1].push(item as never);
                  return rows;
                }, [])
                .map((pair, ri) => (
                  <tr key={ri}>
                    {pair.map(([label, value]) => (
                      <Fragment key={label}>
                        <td className="py-0.5 pr-2 text-slate-500 bg-slate-50 px-2 font-medium whitespace-nowrap">
                          {label}
                        </td>
                        <td className="py-0.5 pr-4 break-all">{value ?? "-"}</td>
                      </Fragment>
                    ))}
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </header>
      {order.map((key) => nodes[key] ?? null)}
    </>
  );
}
