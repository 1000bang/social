import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function Layout() {
	const { logout } = useAuth();
	const [menuOpen, setMenuOpen] = useState(false);

	const navClass = ({ isActive }: { isActive: boolean }) => (isActive ? "active" : "");

	return (
		<div>
			<nav className="navbar">
				<span className="brand">mySocial</span>
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
