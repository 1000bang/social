import { useEffect, useState } from "react";
import { api } from "../api/client";
import type { RecoveryCardResponse, RecoveryCommentResponse } from "../api/types";

export function RecoveryPage() {
	const [cards, setCards] = useState<RecoveryCardResponse[]>([]);
	const [loadingCards, setLoadingCards] = useState(true);
	const [selectedCard, setSelectedCard] = useState<RecoveryCardResponse | null>(null);
	const [comments, setComments] = useState<RecoveryCommentResponse[]>([]);
	const [loadingComments, setLoadingComments] = useState(false);
	const [processingKey, setProcessingKey] = useState<string | null>(null);

	const loadCards = () => {
		setLoadingCards(true);
		api
			.getRecoveryCards()
			.then(setCards)
			.catch(() => {})
			.finally(() => setLoadingCards(false));
	};

	useEffect(() => {
		loadCards();
	}, []);

	const openCard = (card: RecoveryCardResponse) => {
		setSelectedCard(card);
		setComments([]);
		setLoadingComments(true);
		api
			.getRecoveryComments(card.postId)
			.then(setComments)
			.catch(() => {})
			.finally(() => setLoadingComments(false));
	};

	const handleProcessComment = async (commentId: string) => {
		if (!selectedCard) return;
		const key = commentId;
		setProcessingKey(key);
		try {
			await api.processRecoveryComment(selectedCard.postId, commentId);
			setComments((prev) => prev.filter((c) => c.commentId !== commentId));
			setCards((prev) =>
				prev.map((c) =>
					c.postId === selectedCard.postId ? { ...c, commentCount: Math.max(0, c.commentCount - 1) } : c,
				),
			);
		} catch (err) {
			alert(err instanceof Error ? err.message : "처리에 실패했습니다");
		} finally {
			setProcessingKey(null);
		}
	};

	const handleProcessAll = async () => {
		if (!selectedCard) return;
		if (!window.confirm("미처리 댓글을 모두 처리하시겠습니까?")) return;
		setProcessingKey("all");
		try {
			await api.processRecoveryPostAll(selectedCard.postId);
			setComments([]);
			setCards((prev) => prev.map((c) => (c.postId === selectedCard.postId ? { ...c, commentCount: 0 } : c)));
		} catch (err) {
			alert(err instanceof Error ? err.message : "일괄 처리에 실패했습니다");
		} finally {
			setProcessingKey(null);
		}
	};

	if (selectedCard) {
		return (
			<div>
				<div className="page-header">
					<button onClick={() => setSelectedCard(null)}>← 목록으로</button>
					<h2>{selectedCard.templateName}</h2>
					<button className="primary-button" onClick={handleProcessAll} disabled={processingKey !== null || comments.length === 0}>
						{processingKey === "all" ? "처리 중..." : "일괄 처리"}
					</button>
				</div>

				{loadingComments && <p>불러오는 중...</p>}
				{!loadingComments && comments.length === 0 && <p>미처리 댓글이 없습니다.</p>}

				<ul className="recovery-comment-list">
					{comments.map((comment) => (
						<li key={comment.commentId} className="recovery-comment-item">
							<div>
								<span className="recovery-comment-author">{comment.authorUsername ?? "(알 수 없음)"}</span>
								<span className="recovery-comment-text">{comment.text}</span>
								<span className="hint">{new Date(comment.timestamp).toLocaleString("ko-KR")}</span>
							</div>
							<button
								onClick={() => handleProcessComment(comment.commentId)}
								disabled={processingKey !== null}
							>
								{processingKey === comment.commentId ? "처리 중..." : "답글, DM 보내기"}
							</button>
						</li>
					))}
				</ul>
			</div>
		);
	}

	return (
		<div>
			<div className="page-header">
				<h2>미처리 댓글 대응</h2>
				<button onClick={loadCards} disabled={loadingCards}>
					새로고침
				</button>
			</div>
			<p className="hint">템플릿을 선택하면 미처리 댓글을 확인할 수 있습니다.</p>

			{loadingCards && <p>불러오는 중...</p>}
			{!loadingCards && cards.length === 0 && <p>등록된 템플릿이 없습니다.</p>}

			<div className="recovery-card-grid">
				{cards.map((card) => (
					<div key={card.postId} className="recovery-card" onClick={() => openCard(card)}>
						<div className="recovery-card-header">
							{card.thumbnailUrl ? (
								<img src={card.thumbnailUrl} alt={card.templateName} className="recovery-card-thumbnail" />
							) : (
								<div className="recovery-card-thumbnail recovery-card-thumbnail-placeholder">이미지 없음</div>
							)}
							<div>
								<strong>{card.templateName}</strong>
								<p className="hint">
									{card.commentCount > 0 ? `미처리 ${card.commentCount}개` : "미처리 없음"}
								</p>
							</div>
							<span style={{ marginLeft: "auto", color: "var(--text-muted)" }}>›</span>
						</div>
					</div>
				))}
			</div>
		</div>
	);
}
