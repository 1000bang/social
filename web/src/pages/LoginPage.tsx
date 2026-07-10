import { useEffect, useState } from "react";
import { api } from "../api/client";

export function LoginPage() {
	const [loginUrl, setLoginUrl] = useState<string | null>(null);
	const [error, setError] = useState<string | null>(null);

	useEffect(() => {
		api
			.getLoginUrl()
			.then(({ url }) => setLoginUrl(url))
			.catch((err) => setError(err instanceof Error ? err.message : "로그인 URL을 가져오지 못했습니다"));
	}, []);

	return (
		<div className="centered-page">
			<h1>mySocial</h1>
			<p>Instagram 계정으로 로그인해서 댓글/DM 자동 응답 템플릿을 관리하세요.</p>
			{error && <p className="error">{error}</p>}
			{/* 버튼 클릭 후 JS로 location.href를 할당하는 방식은 iOS Chrome에서
			    Universal Link가 인스타그램 앱으로 가로채면서 OAuth 콜백이 깨지는 문제가 있어,
			    사용자가 직접 탭하는 <a> 링크로 이동시킨다. */}
			<a href={loginUrl ?? undefined} aria-disabled={!loginUrl} className={!loginUrl ? "disabled-link" : undefined}>
				{loginUrl ? "Instagram으로 로그인" : "이동 준비 중..."}
			</a>
		</div>
	);
}
