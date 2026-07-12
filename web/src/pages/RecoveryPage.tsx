import { useEffect, useState } from "react";
import { api } from "../api/client";
import type { RecoveryCardResponse } from "../api/types";

export function RecoveryPage() {
	const [cards, setCards] = useState<RecoveryCardResponse[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [processingKey, setProcessingKey] = useState<string | null>(null);

	const load = () => {
		setLoading(true);
		setError(null);
		api
			.getRecoveryCards()
			.then(setCards)
			.catch((err) => setError(err instanceof Error ? err.message : "미처리 댓글을 불러오지 못했습니다"))
			.finally(() => setLoading(false));
	};

	useEffect(() => {
		load();
	}, []);

	const handleProcessComment = async (postId: number, commentId: string) => {
		const key = `${postId}-${commentId}`;
		setProcessingKey(key);
		try {
			await api.processRecoveryComment(postId, commentId);
			setCards((prev) =>
				prev
					.map((card) =>
						card.postId === postId
							? { ...card, comments: card.comments.filter((c) => c.commentId !== commentId) }
							: card,
					)
					.filter((card) => card.comments.length > 0),
			);
		} catch (err) {
			alert(err instanceof Error ? err.message : "처리에 실패했습니다");
		} finally {
			setProcessingKey(null);
		}
	};

	const handleProcessAll = async (postId: number) => {
		if (!window.confirm("이 게시물의 미처리 댓글을 모두 처리하시겠습니까?")) return;
		setProcessingKey(`all-${postId}`);
		try {
			await api.processRecoveryPostAll(postId);
			setCards((prev) => prev.filter((card) => card.postId !== postId));
		} catch (err) {
			alert(err instanceof Error ? err.message : "일괄 처리에 실패했습니다");
		} finally {
			setProcessingKey(null);
		}
	};

	return (
		<div>
			<div className="page-header">
				<h2>미처리 댓글 대응</h2>
				<button onClick={load} disabled={loading}>
					새로고침
				</button>
			</div>
			<p className="hint">
				서버가 잠시 멈췄을 때처럼, 마지막으로 처리한 댓글 이후에 달렸지만 아직 답글/DM이 안 나간 댓글을 게시물별로 모아 보여줍니다.
			</p>

			{loading && <p>불러오는 중...</p>}
			{error && <p className="error">{error}</p>}
			{!loading && !error && cards.length === 0 && <p>미처리 댓글이 없습니다.</p>}

			{cards.map((card) => (
				<div key={card.postId} className="recovery-card">
					<div className="recovery-card-header">
						{card.thumbnailUrl ? (
							<img src={card.thumbnailUrl} alt={card.templateName} className="recovery-card-thumbnail" />
						) : (
							<div className="recovery-card-thumbnail recovery-card-thumbnail-placeholder">이미지 없음</div>
						)}
						<div>
							<strong>{card.templateName}</strong>
							<p className="hint">미처리 댓글 {card.comments.length}개</p>
						</div>
						<button
							className="primary-button"
							onClick={() => handleProcessAll(card.postId)}
							disabled={processingKey !== null}
						>
							{processingKey === `all-${card.postId}` ? "처리 중..." : "일괄 처리"}
						</button>
					</div>

					<ul className="recovery-comment-list">
						{card.comments.map((comment) => (
							<li key={comment.commentId} className="recovery-comment-item">
								<div>
									<span className="recovery-comment-author">{comment.authorUsername ?? "(알 수 없음)"}</span>
									<span className="recovery-comment-text">{comment.text}</span>
									<span className="hint">{new Date(comment.timestamp).toLocaleString("ko-KR")}</span>
								</div>
								<button
									onClick={() => handleProcessComment(card.postId, comment.commentId)}
									disabled={processingKey !== null}
								>
									{processingKey === `${card.postId}-${comment.commentId}` ? "처리 중..." : "답글, DM 보내기"}
								</button>
							</li>
						))}
					</ul>
				</div>
			))}
		</div>
	);
}
