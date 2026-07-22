import { useState } from "react";
import { api } from "../api/client";

const FEATURES = ["댓글 키워드 자동 응답", "DM 키워드 자동 응답", "발송 통계 & 인사이트"];

export function LoginPage() {
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);

	const handleLogin = async () => {
		setLoading(true);
		setError(null);
		try {
			const { url } = await api.getLoginUrl();
			window.location.href = url;
		} catch (err) {
			setError(err instanceof Error ? err.message : "로그인 URL을 가져오지 못했습니다");
			setLoading(false);
		}
	};

	return (
		<div className="login-page">
			<div className="login-card">
				<div className="login-logo">
					<img src="/android-icon-192x192.png" alt="mySocial" />
				</div>
				<h1>mySocial</h1>
				<p className="login-desc">Instagram 댓글과 DM을 자동으로 관리하는 나만의 마케팅 도구</p>

				<ul className="login-features">
					{FEATURES.map((feature) => (
						<li key={feature}>
							<svg viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
								<path
									d="M4 10.5l4 4 8-9"
									stroke="currentColor"
									strokeWidth="2"
									strokeLinecap="round"
									strokeLinejoin="round"
								/>
							</svg>
							{feature}
						</li>
					))}
				</ul>

				{error && <p className="error">{error}</p>}
				<button className="ig-login-button" onClick={handleLogin} disabled={loading}>
					{loading ? "이동 중..." : "Instagram으로 로그인"}
				</button>
			</div>
		</div>
	);
}
