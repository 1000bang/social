import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function AuthCallbackPage() {
	const [searchParams] = useSearchParams();
	const { login } = useAuth();
	const navigate = useNavigate();
	const [error, setError] = useState<string | null>(null);

	useEffect(() => {
		const token = searchParams.get("token");
		const err = searchParams.get("error");

		if (token) {
			login(token);
			navigate("/templates", { replace: true });
		} else {
			setError(err ?? "알 수 없는 오류가 발생했습니다");
		}
	}, [searchParams, login, navigate]);

	if (error) {
		return (
			<div className="centered-page">
				<h2>로그인 실패</h2>
				<p className="error">{error}</p>
			</div>
		);
	}

	return <div className="centered-page">로그인 처리 중...</div>;
}
