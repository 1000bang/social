import { useSearchParams } from "react-router-dom";

export function AuthCallbackPage() {
	const [searchParams] = useSearchParams();
	const error = searchParams.get("error");

	return (
		<div className="centered-page">
			<h2>로그인 실패</h2>
			<p className="error">{error ?? "알 수 없는 오류가 발생했습니다"}</p>
		</div>
	);
}
