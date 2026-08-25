import { useQuery } from "@tanstack/react-query";
import { Database, CalendarClock, FileText, Star, Briefcase, ArrowRight, Search, FileSignature } from "lucide-react";
import {
  fetchCrawlStats,
  fetchMyApplications,
  fetchMyResumes,
  fetchMyScraps,
  type ApplicationDto,
  type CrawlerDetail,
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

const STATUS_TEXT_COLORS: Record<string, string> = {
  PREPARING: "text-slate-600 bg-slate-100",
  APPLIED: "text-blue-700 bg-blue-100",
  SCREEN_PASSED: "text-indigo-700 bg-indigo-100",
  INTERVIEW: "text-violet-700 bg-violet-100",
  OFFER: "text-emerald-700 bg-emerald-100",
  REJECTED: "text-red-600 bg-red-100",
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

function fmtDate(iso: string | null) {
  return iso ? new Date(iso).toLocaleDateString("ko-KR", { month: "short", day: "numeric" }) : "-";
}

function formatCron(expr: string | null): string {
  if (!expr) return "미설정";
  const parts = expr.trim().split(/\s+/);
  if (parts.length < 5) return expr;
  const [min, hour, , , dow] = parts;

  const dayMap: Record<string, string> = {
    "0": "일", "1": "월", "2": "화", "3": "수", "4": "목", "5": "금", "6": "토",
    "1-5": "월~금", "0,6": "주말",
  };
  const hourStr = hour === "*" ? "" : hour === "0" ? "오전 12시" : Number(hour) < 12 ? `오전 ${Number(hour)}시` : `오후 ${Number(hour) - 12}시`;
  const minStr = min === "0" ? "정각" : min === "*" ? "" : `${min}분`;
  const dowStr = dow === "*" ? "매일" : dayMap[dow] ?? `요일(${dow})`;

  if (min !== "*" && hour !== "*" && dow === "*") return `${dowStr} ${hourStr}${minStr ? " " + minStr : ""}`;
  if (min === "0" && hour !== "*") return `${dowStr} ${hourStr}`;
  if (min !== "0" && hour !== "*") return `${dowStr} ${hourStr} ${minStr}`;
  if (min === "*" && hour === "*") return "매시간";
  return expr;
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
  const recentScraps = (scrapsQ.data ?? []).slice(0, 6);
  const crawlerList: CrawlerDetail[] = crawlQ.data?.details ?? [];

  return (
    <div className="max-w-6xl mx-auto p-6 sm:p-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-800">
          {user ? `안녕하세요, ${user.name}님` : "개요"}
        </h1>
        <p className="text-slate-500 mt-1 text-sm">플랫폼 전체 현황을 한눈에 봅니다</p>
      </div>

      {/* 모듈 바로가기 배너 */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <a
          href="/scraper/"
          className="group bg-gradient-to-br from-blue-600 to-blue-700 text-white rounded-xl p-5 shadow-sm hover:shadow-md transition-all flex items-center gap-4"
        >
          <div className="w-11 h-11 bg-white/20 rounded-lg flex items-center justify-center shrink-0">
            <Search size={22} />
          </div>
          <div className="min-w-0">
            <p className="font-semibold text-base">스크래퍼</p>
            <p className="text-blue-100 text-xs">채용공고 수집 · 검색 · 크롤링</p>
          </div>
          <ArrowRight size={18} className="ml-auto text-blue-200 group-hover:text-white transition-colors shrink-0" />
        </a>
        <a
          href="/resume/"
          className="group bg-gradient-to-br from-green-600 to-green-700 text-white rounded-xl p-5 shadow-sm hover:shadow-md transition-all flex items-center gap-4"
        >
          <div className="w-11 h-11 bg-white/20 rounded-lg flex items-center justify-center shrink-0">
            <FileSignature size={22} />
          </div>
          <div className="min-w-0">
            <p className="font-semibold text-base">이력서</p>
            <p className="text-green-100 text-xs">이력서 관리 · 공고 탐색 · 지원 관리</p>
          </div>
          <ArrowRight size={18} className="ml-auto text-green-200 group-hover:text-white transition-colors shrink-0" />
        </a>
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

      {/* 수집 현황 + 지원 현황 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-slate-800 flex items-center gap-2">
              <div className="w-7 h-7 bg-cyan-100 rounded-lg flex items-center justify-center">
                <CalendarClock size={14} className="text-cyan-600" />
              </div>
              수집 현황
            </h2>
            <a href="/scraper/" className="text-xs text-cyan-600 hover:text-cyan-700 flex items-center font-medium">
              뷰어 <ArrowRight size={12} />
            </a>
          </div>
          {crawlerList.length === 0 ? (
            <p className="text-sm text-slate-400 py-6 text-center">등록된 크롤러가 없습니다</p>
          ) : (
            <div className="divide-y divide-slate-100">
              {crawlerList.map((c) => (
                <div key={c.name} className="py-3 first:pt-0 last:pb-0">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-sm font-medium text-slate-700 truncate">{c.name}</span>
                    <span className="text-xs text-slate-400 shrink-0 ml-3">
                      {c.todayCount > 0 ? (
                        <span className="text-cyan-600 font-semibold">+{c.todayCount}</span>
                      ) : (
                        <span>0</span>
                      )}
                      {" · "}
                      <span>{c.total.toLocaleString()}</span>
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-400 truncate">
                    {formatCron(c.schedule)}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-slate-800 flex items-center gap-2">
              <div className="w-7 h-7 bg-indigo-100 rounded-lg flex items-center justify-center">
                <Briefcase size={14} className="text-indigo-600" />
              </div>
              지원 현황
            </h2>
            <a href="/resume/#/applications" className="text-xs text-indigo-600 hover:text-indigo-700 flex items-center font-medium">
              관리 <ArrowRight size={12} />
            </a>
          </div>
          {apps.length === 0 ? (
            <p className="text-sm text-slate-400 py-8 text-center">아직 등록된 지원이 없습니다</p>
          ) : (
            <div className="space-y-2.5">
              {STATUS_ORDER.map((s) => (
                <div key={s} className="flex items-center gap-3">
                  <span className="w-16 text-xs text-slate-500 shrink-0">{STATUS_LABELS[s]}</span>
                  <div className="flex-1 h-5 bg-slate-100 rounded-md overflow-hidden">
                    <div
                      className={`${STATUS_COLORS[s]} h-full rounded-md transition-all`}
                      style={{
                        width: `${(statusCounts[s] / maxCount) * 100}%`,
                        minWidth: statusCounts[s] > 0 ? "8px" : 0,
                      }}
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
      </div>

      {/* 최근 지원 + 최근 스크랩 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-slate-800 flex items-center gap-2">
              <div className="w-7 h-7 bg-indigo-100 rounded-lg flex items-center justify-center">
                <FileText size={14} className="text-indigo-600" />
              </div>
              지원 관리
            </h2>
            <a href="/resume/#/applications" className="text-xs text-indigo-600 hover:text-indigo-700 flex items-center font-medium">
              전체 보기 <ArrowRight size={12} />
            </a>
          </div>
          {recentApps.length === 0 ? (
            <p className="text-sm text-slate-400 py-8 text-center">지원 이력이 없습니다</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {recentApps.map((a) => (
                <li key={a.id} className="py-2.5 flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-slate-700 truncate">{a.companyName}</p>
                    <p className="text-xs text-slate-400 truncate">{a.postingTitle}</p>
                  </div>
                  <div className="shrink-0 text-right">
                    <span className={`inline-block px-2 py-0.5 rounded-full text-[11px] font-medium ${STATUS_TEXT_COLORS[a.status] ?? "bg-slate-100 text-slate-600"}`}>
                      {STATUS_LABELS[a.status] ?? a.status}
                    </span>
                    <p className="text-[11px] text-slate-400 mt-0.5">{fmtDate(a.appliedAt)}</p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-slate-800 flex items-center gap-2">
              <div className="w-7 h-7 bg-amber-100 rounded-lg flex items-center justify-center">
                <Star size={14} className="text-amber-600" />
              </div>
              내 스크랩
            </h2>
            <a href="/resume/#/postings" className="text-xs text-amber-600 hover:text-amber-700 flex items-center font-medium">
              전체 보기 <ArrowRight size={12} />
            </a>
          </div>
          {recentScraps.length === 0 ? (
            <p className="text-sm text-slate-400 py-8 text-center">스크랩한 공고가 없습니다</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {recentScraps.map((s) => (
                <li key={s.id} className="py-2.5">
                  <div className="flex items-start gap-2">
                    <span className="text-amber-400 text-sm mt-0.5 shrink-0">★</span>
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium text-slate-700 truncate">{s.company}</p>
                      <p className="text-xs text-slate-500 truncate">{s.position}</p>
                      <div className="flex items-center gap-2 mt-1">
                        {s.siteName && (
                          <span className={`inline-block px-1.5 py-0.5 rounded text-[10px] font-medium ${
                            s.siteName === "saramin" ? "bg-blue-100 text-blue-700" :
                            s.siteName === "jobkorea" ? "bg-green-100 text-green-700" :
                            "bg-slate-100 text-slate-600"
                          }`}>
                            {s.siteName}
                          </span>
                        )}
                        <span className="text-[11px] text-slate-400">{fmtDate(s.scrappedAt)}</span>
                      </div>
                    </div>
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
