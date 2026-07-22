import { useEffect } from "react";
import { Navigate, useSearchParams } from "react-router-dom";
import { POPUP_WINDOW_NAME } from "./LoginPage";

export function AuthCallbackPage() {
	const [searchParams] = useSearchParams();
	const error = searchParams.get("error");
	// 팝업이 인스타그램 도메인을 거치는 동안 브라우저가 window.opener를 끊어버리는 경우가 있어,
	// 팝업 생성 시 지정한 창 이름도 함께 확인해 팝업 여부를 판단한다.
	const isPopupWindow = window.opener != null || window.name === POPUP_WINDOW_NAME;

	useEffect(() => {
		if (!isPopupWindow) return;
		if (window.opener) {
			window.opener.postMessage(
				error ? { type: "instagram-login", status: "error", message: error } : { type: "instagram-login", status: "success" },
				window.location.origin,
			);
		}
		// opener가 끊겼으면 메시지를 보낼 수 없지만, 이 창은 이미 할 일을 다 했으니 닫기만 한다.
		// 원래 창은 팝업이 닫히는 것을 감지해 스스로 인증 상태를 다시 확인한다.
		window.close();
	}, [error, isPopupWindow]);

	if (isPopupWindow) {
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
