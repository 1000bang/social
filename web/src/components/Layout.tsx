import { useEffect, useState } from "react";
import { Link, NavLink, Outlet } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../auth/AuthContext";

export function Layout() {
	const { logout } = useAuth();
	const [menuOpen, setMenuOpen] = useState(false);
	const [needsReauth, setNeedsReauth] = useState(false);
	const [reconnecting, setReconnecting] = useState(false);

	const navClass = ({ isActive }: { isActive: boolean }) => (isActive ? "active" : "");

	useEffect(() => {
		api
			.getMe()
			.then((me) => setNeedsReauth(me.status === "NEEDS_REAUTH"))
			.catch(() => setNeedsReauth(false));
	}, []);

	const handleReconnect = async () => {
		setReconnecting(true);
		try {
			const { url } = await api.getLoginUrl();
			window.location.href = url;
		} catch {
			setReconnecting(false);
		}
	};

	return (
		<div>
			{needsReauth && (
				<div className="reauth-banner">
					Instagram 연결이 끊어졌어요. 자동응답이 멈춘 상태이니 다시 연결해주세요.
					<button type="button" onClick={handleReconnect} disabled={reconnecting}>
						{reconnecting ? "이동 중..." : "Instagram 다시 연결하기"}
					</button>
				</div>
			)}
			<nav className="navbar">
				<Link className="brand" to="/home">
					mySocial
				</Link>
				<div className="navbar-links">
					<NavLink to="/templates" className={navClass}>
						템플릿
					</NavLink>
					<NavLink to="/send-logs" className={navClass}>
						발송 통계
					</NavLink>
					<NavLink to="/recovery" className={navClass}>
						미처리 대응
					</NavLink>
					<NavLink to="/settings" className={navClass}>
						환경설정
					</NavLink>
				</div>
				<button className="logout-button" onClick={logout}>
					로그아웃
				</button>
				<button className="hamburger-button" onClick={() => setMenuOpen((v) => !v)} aria-label="메뉴 열기">
					☰
				</button>
				<div className={`mobile-menu ${menuOpen ? "open" : ""}`}>
					<NavLink to="/templates" className={navClass} onClick={() => setMenuOpen(false)}>
						템플릿
					</NavLink>
					<NavLink to="/send-logs" className={navClass} onClick={() => setMenuOpen(false)}>
						발송 통계
					</NavLink>
					<NavLink to="/recovery" className={navClass} onClick={() => setMenuOpen(false)}>
						미처리 대응
					</NavLink>
					<NavLink to="/settings" className={navClass} onClick={() => setMenuOpen(false)}>
						환경설정
					</NavLink>
					<button
						onClick={() => {
							setMenuOpen(false);
							logout();
						}}
					>
						로그아웃
					</button>
				</div>
			</nav>
			<main className="content">
				<Outlet />
			</main>
		</div>
	);
}
