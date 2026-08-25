import { useQuery } from "@tanstack/react-query";
import { Database, CalendarClock, FileText, Star, Briefcase, ArrowRight } from "lucide-react";
import {
  fetchCrawlStats,
  fetchMyApplications,
  fetchMyResumes,
  fetchMyScraps,
  type ApplicationDto,
} from "../api/dashboard";
import { useAuth } from "../hooks/useAuth";

const STATUS_ORDER = ["PREPARING", "APPLIED", "SCREEN_PASSED", "INTERVIEW", "OFFER", "REJECTED"] as const;
const STATUS_LABELS: Record<string, string> = {
  PREPARING: "준비 중",
  APPLIED: "지원 완료",
  SCREEN_PASSED: "서류 통과",
  INTERVIEW: "면접",
  OFFER: "채용 확정",
  REJECTED: "불합격",
};
const STATUS_COLORS: Record<string, string> = {
  PREPARING: "bg-slate-200",
  APPLIED: "bg-blue-500",
  SCREEN_PASSED: "bg-indigo-500",
  INTERVIEW: "bg-violet-500",
  OFFER: "bg-emerald-500",
  REJECTED: "bg-red-400",
};

function StatCard({
  icon: Icon,
  label,
  value,
  sub,
  color,
}: {
  icon: typeof Database;
  label: string;
  value: string | number;
  sub?: string;
  color: string;
}) {
  return (
    <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
      <div className="flex items-center gap-3">
        <div className={`w-10 h-10 ${color} rounded-lg flex items-center justify-center shrink-0`}>
          <Icon className="text-white" size={20} />
        </div>
        <div className="min-w-0">
          <p className="text-xs text-slate-500">{label}</p>
          <p className="text-xl font-bold text-slate-800 leading-tight">{value}</p>
        </div>
      </div>
      {sub && <p className="text-[11px] text-slate-400 mt-2 truncate">{sub}</p>}
    </div>
  );
}

export default function Dashboard() {
  const { user } = useAuth();

  const crawlQ = useQuery({ queryKey: ["dash-crawl"], queryFn: fetchCrawlStats, retry: 1 });
  const resumesQ = useQuery({ queryKey: ["dash-resumes"], queryFn: fetchMyResumes, retry: 1 });
  const appsQ = useQuery({ queryKey: ["dash-apps"], queryFn: fetchMyApplications, retry: 1 });
  const scrapsQ = useQuery({ queryKey: ["dash-scraps"], queryFn: fetchMyScraps, retry: 1 });

  const apps: ApplicationDto[] = appsQ.data ?? [];
  const statusCounts = Object.fromEntries(
    STATUS_ORDER.map((s) => [s, apps.filter((a) => a.status === s).length])
  ) as Record<(typeof STATUS_ORDER)[number], number>;
  const maxCount = Math.max(1, ...Object.values(statusCounts));
  const recentApps = [...apps]
    .sort((a, b) => (b.appliedAt ?? "").localeCompare(a.appliedAt ?? ""))
    .slice(0, 5);
  const recentScraps = (scrapsQ.data ?? []).slice(0, 4);

  const fmtDate = (iso: string | null) =>
    iso ? new Date(iso).toLocaleDateString("ko-KR", { month: "short", day: "numeric" }) : "-";

  return (
    <div className="max-w-6xl mx-auto p-6 sm:p-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-800">
          {user ? `안녕하세요, ${user.name}님` : "개요"}
        </h1>
        <p className="text-slate-500 mt-1 text-sm">플랫폼 전체 현황을 한눈에 봅니다</p>
      </div>

      {/* 현황 카드 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          icon={Database}
          label="수집 공고"
          value={crawlQ.isLoading ? "-" : crawlQ.data?.totalPostings.toLocaleString() ?? "-"}
          sub={
            crawlQ.data?.lastCrawledAt
              ? `최근 수집 ${fmtDate(crawlQ.data.lastCrawledAt)}`
              : undefined
          }
          color="bg-blue-500"
        />
        <StatCard
          icon={CalendarClock}
          label="오늘 수집"
          value={crawlQ.isLoading ? "-" : crawlQ.data?.todayPostings ?? "-"}
          sub={
            crawlQ.data
              ? `크롤러 ${crawlQ.data.crawlers}개 · 스케줄 ${crawlQ.data.activeSchedules}개 활성`
              : undefined
          }
          color="bg-cyan-600"
        />
        <StatCard
          icon={FileText}
          label="내 이력서"
          value={resumesQ.isLoading ? "-" : resumesQ.data?.length ?? "-"}
          color="bg-green-600"
        />
        <StatCard
          icon={Star}
          label="내 스크랩"
          value={scrapsQ.isLoading ? "-" : scrapsQ.data?.length ?? "-"}
          color="bg-amber-500"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* 지원 현황 파이프라인 */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-slate-800 flex items-center gap-2">
              <Briefcase size={16} className="text-slate-400" />
              지원 현황
            </h2>
            <a href="/resume/#/applications" className="text-xs text-blue-600 hover:text-blue-700 flex items-center">
              관리 <ArrowRight size={12} />
            </a>
          </div>
          {apps.length === 0 ? (
            <p className="text-sm text-slate-400 py-8 text-center">
              아직 등록된 지원이 없습니다
            </p>
          ) : (
            <div className="space-y-2.5">
              {STATUS_ORDER.map((s) => (
                <div key={s} className="flex items-center gap-3">
                  <span className="w-16 text-xs text-slate-500 shrink-0">{STATUS_LABELS[s]}</span>
                  <div className="flex-1 h-5 bg-slate-100 rounded-md overflow-hidden">
                    <div
                      className={`${STATUS_COLORS[s]} h-full rounded-md transition-all`}
                      style={{ width: `${(statusCounts[s] / maxCount) * 100}%`, minWidth: statusCounts[s] > 0 ? "8px" : 0 }}
                    />
                  </div>
                  <span className="w-6 text-xs font-semibold text-slate-700 text-right shrink-0">
                    {statusCounts[s]}
                  </span>
                </div>
              ))}
              <p className="text-[11px] text-slate-400 pt-1">총 {apps.length}건</p>
            </div>
          )}
        </div>

        {/* 최근 활동 */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-slate-800">최근 지원</h2>
            <a href="/resume/#/postings" className="text-xs text-blue-600 hover:text-blue-700 flex items-center">
              공고 탐색 <ArrowRight size={12} />
            </a>
          </div>
          {recentApps.length === 0 ? (
            <div className="py-4">
              <p className="text-sm text-slate-400 text-center mb-4">최근 지원 이력이 없습니다</p>
              {recentScraps.length > 0 && (
                <>
                  <p className="text-xs font-medium text-slate-500 mb-2 flex items-center gap-1">
                    <Star size={12} className="text-amber-500" /> 최근 스크랩
                  </p>
                  <ul className="space-y-1.5">
                    {recentScraps.map((s) => (
                      <li key={s.id} className="text-sm text-slate-600 truncate">
                        <span className="text-slate-400 text-xs mr-1.5">{s.siteName}</span>
                        {s.company} · {s.position}
                      </li>
                    ))}
                  </ul>
                </>
              )}
            </div>
          ) : (
            <ul className="divide-y divide-slate-100">
              {recentApps.map((a) => (
                <li key={a.id} className="py-2.5 flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-slate-700 truncate">{a.companyName}</p>
                    <p className="text-xs text-slate-400 truncate">{a.postingTitle}</p>
                  </div>
                  <div className="shrink-0 text-right">
                    <span className="inline-block px-2 py-0.5 rounded-full bg-slate-100 text-[11px] text-slate-600">
                      {STATUS_LABELS[a.status] ?? a.status}
                    </span>
                    <p className="text-[11px] text-slate-400 mt-0.5">{fmtDate(a.appliedAt)}</p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {(crawlQ.isError || resumesQ.isError || appsQ.isError || scrapsQ.isError) && (
        <p className="text-xs text-slate-400 text-center">
          일부 데이터를 불러오지 못했습니다. 로그인 상태를 확인해 주세요.
        </p>
      )}
    </div>
  );
}
