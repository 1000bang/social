import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { api } from "../api/client";

export const POPUP_WINDOW_NAME = "instagram-login";

const FEATURES = ["댓글 키워드 자동 응답", "DM 키워드 자동 응답", "발송 통계 & 인사이트"];

export function LoginPage() {
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const navigate = useNavigate();
	const { refresh } = useAuth();
	// 팝업이 메시지로 이미 처리됐는지 표시 — 팝업이 닫힌 뒤의 폴백 확인과 중복 처리되지 않게 한다.
	const handledRef = useRef(false);

	// 모바일에서 인스타그램 로그인 URL로 전체 페이지 이동하면 OS가 인스타그램 앱으로 가로채서
	// 콜백이 끊기는 문제가 있어, 팝업으로 열고 postMessage로 완료를 전달받는다.
	// 팝업은 페이지를 새로고침하지 않으므로, 인증 상태를 명시적으로 갱신한 뒤에 이동해야 한다
	// (안 그러면 ProtectedRoute가 로그인 전 상태를 그대로 보고 다시 /login으로 돌려보낸다).
	useEffect(() => {
		const handleMessage = async (event: MessageEvent) => {
			if (event.origin !== window.location.origin) return;
			if (event.data?.type !== "instagram-login") return;
			if (handledRef.current) return;
			handledRef.current = true;
			setLoading(false);
			if (event.data.status === "success") {
				const authenticated = await refresh();
				if (authenticated) {
					navigate("/home", { replace: true });
				} else {
					setError("로그인 확인에 실패했습니다. 다시 시도해주세요.");
				}
			} else {
				setError(event.data.message ?? "로그인에 실패했습니다");
			}
		};
		window.addEventListener("message", handleMessage);
		return () => window.removeEventListener("message", handleMessage);
	}, [navigate, refresh]);

	const handleLogin = async () => {
		setLoading(true);
		setError(null);
		handledRef.current = false;
		// 팝업은 클릭 이벤트 안에서 동기적으로 열어야 한다 (특히 iOS Safari) — await 이후에 열면 팝업 차단됨.
		// 그래서 빈 창을 먼저 열고, URL을 받아온 뒤 그 창의 위치만 바꾼다.
		const popup = window.open("about:blank", POPUP_WINDOW_NAME, "width=500,height=700");
		try {
			const { url } = await api.getLoginUrl();
			if (!popup) {
				window.location.href = url;
				return;
			}
			popup.location.href = url;
			const checkClosed = window.setInterval(async () => {
				if (!popup.closed) return;
				window.clearInterval(checkClosed);
				if (handledRef.current) return;
				handledRef.current = true;
				// 팝업이 인스타그램 도메인을 거치는 동안 브라우저가 window.opener를 끊어버려
				// 메시지를 못 받는 경우가 있다. 팝업이 닫혔다면 직접 인증 상태를 확인해 복구한다.
				const authenticated = await refresh();
				setLoading(false);
				if (authenticated) navigate("/home", { replace: true });
			}, 500);
		} catch (err) {
			popup?.close();
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
					{loading ? "로그인 중..." : "Instagram으로 로그인"}
				</button>
			</div>
		</div>
	);
}
