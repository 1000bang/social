import { useEffect, useState } from "react";
import { api } from "../api/client";
import type { SendLogResponse } from "../api/types";

export function SendLogsPage() {
	const [logs, setLogs] = useState<SendLogResponse[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);

	useEffect(() => {
		api
			.listSendLogs()
			.then(setLogs)
			.catch((err) => setError(err instanceof Error ? err.message : "발송 통계를 불러오지 못했습니다"))
			.finally(() => setLoading(false));
	}, []);

	return (
		<div>
			<h2>발송 통계</h2>
			{loading && <p>불러오는 중...</p>}
			{error && <p className="error">{error}</p>}
			{!loading && !error && logs.length === 0 && <p>아직 발송 이력이 없습니다.</p>}
			{logs.length > 0 && (
				<table className="data-table">
					<thead>
						<tr>
							<th>템플릿</th>
							<th>대상</th>
							<th>수신자</th>
							<th>결과</th>
							<th>실패 사유</th>
							<th>일시</th>
						</tr>
					</thead>
					<tbody>
						{logs.map((log) => (
							<tr key={log.id}>
								<td>{log.templateName}</td>
								<td>{log.audienceType ?? "-"}</td>
								<td>{log.recipientPlatformUserId}</td>
								<td className={log.result === "SUCCESS" ? "success" : "failure"}>{log.result}</td>
								<td>{log.failureReason ?? "-"}</td>
								<td>{new Date(log.createdAt).toLocaleString("ko-KR")}</td>
							</tr>
						))}
					</tbody>
				</table>
			)}
		</div>
	);
}
