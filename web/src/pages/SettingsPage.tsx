import { useEffect, useState, type FormEvent } from "react";
import { api } from "../api/client";
import type { AccountSettingsResponse } from "../api/types";

const MIN_POST_PICKER_LIMIT = 1;
const MAX_POST_PICKER_LIMIT = 25;
const MIN_MAX_MESSAGES_PER_AUDIENCE = 1;
const MAX_MAX_MESSAGES_PER_AUDIENCE = 5;

export function SettingsPage() {
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [saving, setSaving] = useState(false);
	const [saveMessage, setSaveMessage] = useState<string | null>(null);

	const [commentReplyText, setCommentReplyText] = useState("");
	const [nonKeywordCommentReplyText, setNonKeywordCommentReplyText] = useState("");
	const [nonFollowerMessageText, setNonFollowerMessageText] = useState("");
	const [postPickerLimit, setPostPickerLimit] = useState(5);
	const [maxMessagesPerAudience, setMaxMessagesPerAudience] = useState(3);

	const applySettings = (settings: AccountSettingsResponse) => {
		setCommentReplyText(settings.commentReplyText ?? "");
		setNonKeywordCommentReplyText(settings.nonKeywordCommentReplyText ?? "");
		setNonFollowerMessageText(settings.nonFollowerMessageText ?? "");
		setPostPickerLimit(settings.postPickerLimit);
		setMaxMessagesPerAudience(settings.maxMessagesPerAudience);
	};

	useEffect(() => {
		api
			.getSettings()
			.then(applySettings)
			.catch((err) => setError(err instanceof Error ? err.message : "환경설정을 불러오지 못했습니다"))
			.finally(() => setLoading(false));
	}, []);

	const handleSubmit = async (e: FormEvent) => {
		e.preventDefault();
		setError(null);
		setSaveMessage(null);
		setSaving(true);
		try {
			const settings = await api.updateSettings({
				commentReplyText,
				nonKeywordCommentReplyText,
				nonFollowerMessageText,
				postPickerLimit,
				maxMessagesPerAudience,
			});
			applySettings(settings);
			setSaveMessage("저장되었습니다");
		} catch (err) {
			setError(err instanceof Error ? err.message : "저장에 실패했습니다");
		} finally {
			setSaving(false);
		}
	};

	if (loading) return <p>불러오는 중...</p>;

	return (
		<div>
			<div className="page-header">
				<h2>환경설정</h2>
			</div>

			<form onSubmit={handleSubmit} className="template-form">
				{error && <p className="error">{error}</p>}
				{saveMessage && <p className="success">{saveMessage}</p>}

				<fieldset className="message-fieldset">
					<legend>기본 문구 설정</legend>
					<p className="hint">템플릿 생성 폼을 열 때 아래 값이 초기값으로 채워집니다. (이미 생성된 템플릿에는 영향을 주지 않습니다)</p>

					<label>
						1. 키워드 댓글에 대한 응답 문구
						<input
							value={commentReplyText}
							onChange={(e) => setCommentReplyText(e.target.value)}
							placeholder="메시지 보냈어요! DM이 안보이면 말씀주세요!"
						/>
					</label>

					<label>
						2. 비키워드 댓글에 대한 응답 문구
						<input
							value={nonKeywordCommentReplyText}
							onChange={(e) => setNonKeywordCommentReplyText(e.target.value)}
							placeholder="댓글 남겨주셔서 감사합니다!"
						/>
					</label>

					<label>
						3. 논팔로워 DM 기본 메시지
						<input
							value={nonFollowerMessageText}
							onChange={(e) => setNonFollowerMessageText(e.target.value)}
							placeholder="팔로우가 확인되지 않았어요! 팔로우 후 다시 요청 부탁드립니다."
						/>
					</label>
				</fieldset>

				<fieldset className="message-fieldset">
					<legend>게시물 선택 설정</legend>
					<label>
						한번에 보여질 게시글 수 ({MIN_POST_PICKER_LIMIT}~{MAX_POST_PICKER_LIMIT})
						<input
							type="number"
							min={MIN_POST_PICKER_LIMIT}
							max={MAX_POST_PICKER_LIMIT}
							value={postPickerLimit}
							onChange={(e) => setPostPickerLimit(Number(e.target.value))}
							required
						/>
					</label>
				</fieldset>

				<fieldset className="message-fieldset">
					<legend>메시지 설정</legend>
					<label>
						팔로워/논팔로워에게 보낼 메시지 최대 개수 ({MIN_MAX_MESSAGES_PER_AUDIENCE}~{MAX_MAX_MESSAGES_PER_AUDIENCE})
						<input
							type="number"
							min={MIN_MAX_MESSAGES_PER_AUDIENCE}
							max={MAX_MAX_MESSAGES_PER_AUDIENCE}
							value={maxMessagesPerAudience}
							onChange={(e) => setMaxMessagesPerAudience(Number(e.target.value))}
							required
						/>
					</label>
				</fieldset>

				<button type="submit" disabled={saving}>
					{saving ? "저장 중..." : "저장"}
				</button>
			</form>
		</div>
	);
}
