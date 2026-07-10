export function LoginPage() {
	return (
		<div className="centered-page">
			<h1>mySocial</h1>
			<p>Instagram 계정으로 로그인해서 댓글/DM 자동 응답 템플릿을 관리하세요.</p>
			{/* www.instagram.com으로 바로 이동하면 iOS Chrome에서 Universal Link가 인스타그램
			    앱으로 가로채서 OAuth 콜백이 깨지는 문제가 있어, 우리 서버(/redirect)를 한 번
			    거쳐서 이동시킨다. */}
			<a href="/api/auth/instagram/redirect">Instagram으로 로그인</a>
		</div>
	);
}
