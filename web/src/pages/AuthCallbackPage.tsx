import { useEffect } from "react";
import { Navigate, useSearchParams } from "react-router-dom";

export function AuthCallbackPage() {
	const [searchParams] = useSearchParams();
	const error = searchParams.get("error");
	const isPopup = window.opener != null;

	useEffect(() => {
		if (!isPopup) return;
		window.opener.postMessage(
			error ? { type: "instagram-login", status: "error", message: error } : { type: "instagram-login", status: "success" },
			window.location.origin,
		);
		window.close();
	}, [error, isPopup]);

	if (isPopup) {
		return <div className="centered-page">로그인 처리 중...</div>;
	}

	// 팝업이 차단되어 전체 페이지 이동으로 대체된 경우의 폴백
	if (error) {
		return (
			<div className="centered-page">
				<h2>로그인 실패</h2>
				<p className="error">{error}</p>
			</div>
		);
	}
	return <Navigate to="/home" replace />;
}
