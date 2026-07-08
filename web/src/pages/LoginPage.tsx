import { useState } from "react";
import { api } from "../api/client";

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
		<div className="centered-page">
			<h1>mySocial</h1>
			<p>Instagram 계정으로 로그인해서 댓글/DM 자동 응답 템플릿을 관리하세요.</p>
			{error && <p className="error">{error}</p>}
			<button onClick={handleLogin} disabled={loading}>
				{loading ? "이동 중..." : "Instagram으로 로그인"}
			</button>
		</div>
	);
}
