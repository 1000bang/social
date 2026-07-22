import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import type {
	FollowerStatsResponse,
	RecoveryCardResponse,
	SendLogResponse,
	SendLogSummaryResponse,
	TemplateRankingResponse,
} from "../api/types";

const INSIGHT_DISPLAY_COUNT = 2;

function pickRandom<T>(items: T[], count: number): T[] {
	const shuffled = [...items];
	for (let i = shuffled.length - 1; i > 0; i--) {
		const j = Math.floor(Math.random() * (i + 1));
		[shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
	}
	return shuffled.slice(0, count);
}

function DeltaValue({ delta }: { delta: number | null | undefined }) {
	if (delta === null || delta === undefined || delta === 0) return <span>-</span>;
	if (delta > 0) return <span className="follower-delta-up">▲{delta}</span>;
	return <span className="follower-delta-down">▼{Math.abs(delta)}</span>;
}

function formatMonthDay(isoDate: string): string {
	const [, month, day] = isoDate.split("-");
	return `${month}.${day}`;
}

function trendClause(diff: number, direction: "up" | "down", previousRange: string): string {
	if (diff === 0) return `저번 주(${previousRange})랑 비슷한 추세예요 🙂`;
	if (direction === "up") {
		return diff > 0
			? `저번 주(${previousRange})보다 ${diff}명 더 늘었어요 😆`
			: `저번 주(${previousRange})보다 ${Math.abs(diff)}명 덜 늘었어요 🙂`;
	}
	return diff > 0
		? `저번 주(${previousRange})보다 ${diff}명 덜 줄었어요 🙂`
		: `저번 주(${previousRange})보다 ${Math.abs(diff)}명 더 줄었어요 😭`;
}

function formatFollowerInsight(stats: FollowerStatsResponse | null): string | null {
	if (!stats || stats.weekDelta === null || stats.weekRangeStart === null || stats.weekRangeEnd === null) return null;
	const range = `${formatMonthDay(stats.weekRangeStart)}~${formatMonthDay(stats.weekRangeEnd)}`;
	const diff = stats.previousWeekDelta !== null ? stats.weekDelta - stats.previousWeekDelta : null;
	const previousRange =
		stats.previousWeekRangeStart !== null && stats.previousWeekRangeEnd !== null
			? `${formatMonthDay(stats.previousWeekRangeStart)}~${formatMonthDay(stats.previousWeekRangeEnd)}`
			: null;

	if (stats.weekDelta >= 0) {
		const base = `이번 주(${range})에 팔로워가 ${stats.weekDelta}명 증가했어요.😁`;
		return diff === null || previousRange === null ? base : `${base} ${trendClause(diff, "up", previousRange)}`;
	}
	const decrease = Math.abs(stats.weekDelta);
	const base = `이번 주(${range})에 팔로워가 ${decrease}명 감소했어요.😢`;
	return diff === null || previousRange === null ? base : `${base} ${trendClause(diff, "down", previousRange)}`;
}

const QUICK_LINKS = [
	{ to: "/templates", label: "템플릿 관리", desc: "댓글/DM 키워드 자동 응답 템플릿을 만들고 관리해요" },
	{ to: "/send-logs", label: "발송 통계", desc: "발송 추이와 템플릿별 성과를 확인해요" },
	{ to: "/recovery", label: "미처리 대응", desc: "놓친 댓글에 대한 응답을 확인하고 처리해요" },
	{ to: "/settings", label: "환경설정", desc: "기본 응답 문구와 발송 옵션을 설정해요" },
];

export function HomePage() {
	const { me } = useAuth();
	const [summary, setSummary] = useState<SendLogSummaryResponse | null>(null);
	const [insights, setInsights] = useState<string[]>([]);
	const [topTemplates, setTopTemplates] = useState<TemplateRankingResponse[]>([]);
	const [recentLogs, setRecentLogs] = useState<SendLogResponse[]>([]);
	const [templateCount, setTemplateCount] = useState<number | null>(null);
	const [unprocessedCount, setUnprocessedCount] = useState<number | null>(null);
	const [followerStats, setFollowerStats] = useState<FollowerStatsResponse | null>(null);

	useEffect(() => {
		api.getFollowerGrowth().then(setFollowerStats).catch(() => setFollowerStats(null));
	}, []);

	useEffect(() => {
		api.getSendLogSummary().then(setSummary).catch(() => setSummary(null));
	}, []);

	useEffect(() => {
		api
			.getSendLogInsights()
			.then((res) => setInsights(pickRandom(res.map((i) => i.text), INSIGHT_DISPLAY_COUNT)))
			.catch(() => setInsights([]));
	}, []);

	useEffect(() => {
		api
			.getTopTemplates()
			.then((res) => setTopTemplates(res.slice(0, 3)))
			.catch(() => setTopTemplates([]));
	}, []);

	useEffect(() => {
		api
			.listSendLogs(0, 5)
			.then((res) => setRecentLogs(res.content))
			.catch(() => setRecentLogs([]));
	}, []);

	useEffect(() => {
		api
			.listTemplates(0, 1)
			.then((res) => setTemplateCount(res.totalElements))
			.catch(() => setTemplateCount(null));
	}, []);

	useEffect(() => {
		api
			.getRecoveryCards()
			.then((cards: RecoveryCardResponse[]) => setUnprocessedCount(cards.reduce((sum, c) => sum + c.comments.length, 0)))
			.catch(() => setUnprocessedCount(null));
	}, []);

	const followerInsight = formatFollowerInsight(followerStats);
	const displayedInsights = followerInsight ? [followerInsight, ...insights] : insights;

	return (
		<div>
			<div className="home-greeting">
				{me?.profilePictureUrl ? (
					<img className="profile-avatar" src={me.profilePictureUrl} alt={me.username} />
				) : (
					<div className="profile-avatar profile-avatar-placeholder">{me?.username?.[0]?.toUpperCase() ?? "?"}</div>
				)}
				<h2>안녕하세요, {me?.username ?? "..."}님!</h2>
			</div>

			<div className="follower-stat-banner">
				<div className="follower-stat-row">
					<div className="follower-stat-main">
						<div className="follower-stat-count">{followerStats?.currentCount ?? "-"}</div>
						<div className="follower-stat-label">팔로워</div>
					</div>
					<div className="follower-stat-deltas">
						WEEK <DeltaValue delta={followerStats?.weekDelta} /> / MONTH <DeltaValue delta={followerStats?.monthDelta} />
					</div>
				</div>
				<div className="follower-stat-hint">(매일 05시에 업데이트 됩니다.)</div>
			</div>

			{displayedInsights.length > 0 && (
				<div className="insight-list">
					{displayedInsights.map((text) => (
						<p className="insight-item" key={text}>
							{text}
						</p>
					))}
				</div>
			)}

			<div className="summary-cards">
				<div className="summary-card">
					<div className="value">{summary?.contactedUsersThisMonth ?? "-"}</div>
					<div className="label">이번 달 컨택한 사용자 수</div>
				</div>
				<div className="summary-card">
					<div className="value">{summary?.messagesSentThisMonth ?? "-"}</div>
					<div className="label">이번 달 발송한 메시지 수</div>
				</div>
				<div className="summary-card">
					<div className="value">{templateCount ?? "-"}</div>
					<div className="label">전체 템플릿 수</div>
				</div>
				<div className="summary-card">
					<div className="value">{unprocessedCount ?? "-"}</div>
					<div className="label">미처리 댓글 수</div>
				</div>
			</div>

			<div className="home-grid">
				<div className="chart-section">
					<div className="chart-header">
						<strong>템플릿 TOP 3</strong>
					</div>
					{topTemplates.length === 0 && <p className="hint">아직 발송 이력이 없습니다.</p>}
					{topTemplates.length > 0 && (
						<table className="data-table">
							<thead>
								<tr>
									<th>순번</th>
									<th>템플릿명</th>
									<th>메시지 발송수</th>
								</tr>
							</thead>
							<tbody>
								{topTemplates.map((t, index) => (
									<tr key={t.templateId}>
										<td>{index + 1}</td>
										<td>{t.templateName}</td>
										<td>{t.messagesSent}</td>
									</tr>
								))}
							</tbody>
						</table>
					)}
				</div>

				<div className="chart-section">
					<div className="chart-header">
						<strong>최근 발송</strong>
					</div>
					{recentLogs.length === 0 && <p className="hint">아직 발송 이력이 없습니다.</p>}
					{recentLogs.length > 0 && (
						<table className="data-table">
							<thead>
								<tr>
									<th>템플릿</th>
									<th>결과</th>
								</tr>
							</thead>
							<tbody>
								{recentLogs.map((log) => (
									<tr key={log.id}>
										<td>{log.templateName}</td>
										<td className={log.result === "SUCCESS" ? "success" : "failure"}>{log.result}</td>
									</tr>
								))}
							</tbody>
						</table>
					)}
				</div>
			</div>

			<div className="quick-link-grid">
				{QUICK_LINKS.map((link) => (
					<Link className="quick-link-card" to={link.to} key={link.to}>
						<strong>{link.label}</strong>
						<p>{link.desc}</p>
					</Link>
				))}
			</div>
		</div>
	);
}
