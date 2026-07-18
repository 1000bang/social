import { useEffect, useState, type FormEvent, type MouseEvent } from "react";
import { api } from "../api/client";
import { Pagination } from "../components/Pagination";
import type {
	AccountSettingsResponse,
	ButtonInput,
	CreateTemplateRequest,
	MessageInput,
	MessageType,
	PostResponse,
	TemplateDetailResponse,
	TemplateResponse,
} from "../api/types";

const MAX_BUTTONS = 3;

const DEFAULT_MAX_MESSAGES = 3;
const DEFAULT_NON_FOLLOWER_TEXT = "팔로우가 확인되지 않았어요! 팔로우 후 다시 요청 부탁드립니다.";

function emptyMessage(): MessageInput {
	return { messageType: "TEXT", textContent: "" };
}

export function TemplatesPage() {
	const [templates, setTemplates] = useState<TemplateResponse[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [showForm, setShowForm] = useState(false);
	const [editingId, setEditingId] = useState<number | null>(null);
	const [page, setPage] = useState(0);
	const [pageSize, setPageSize] = useState(10);
	const [totalPages, setTotalPages] = useState(0);

	const [posts, setPosts] = useState<PostResponse[]>([]);
	const [postsLoading, setPostsLoading] = useState(false);
	const [postsError, setPostsError] = useState<string | null>(null);

	const [settings, setSettings] = useState<AccountSettingsResponse | null>(null);
	const maxMessages = settings?.maxMessagesPerAudience ?? DEFAULT_MAX_MESSAGES;

	const [name, setName] = useState("");
	const [postId, setPostId] = useState<number | null>(null);
	const [useSchedule, setUseSchedule] = useState(false);
	const [dispatchTime, setDispatchTime] = useState("");
	const [keywordsText, setKeywordsText] = useState("");
	const [useDmKeyword, setUseDmKeyword] = useState(false);
	const [dmKeyword, setDmKeyword] = useState("");
	const [commentReplyText, setCommentReplyText] = useState("");
	const [nonKeywordCommentReplyText, setNonKeywordCommentReplyText] = useState("");
	const [nonKeywordReplyEnabled, setNonKeywordReplyEnabled] = useState(true);
	const [followerMessages, setFollowerMessages] = useState<MessageInput[]>([emptyMessage()]);
	const [nonFollowerMessages, setNonFollowerMessages] = useState<MessageInput[]>([
		{ messageType: "TEXT", textContent: DEFAULT_NON_FOLLOWER_TEXT },
	]);
	const [submitting, setSubmitting] = useState(false);
	const [formError, setFormError] = useState<string | null>(null);
	const [uploadingIndex, setUploadingIndex] = useState<string | null>(null);

	const applyDefaultTexts = (s: AccountSettingsResponse | null) => {
		setCommentReplyText(s?.commentReplyText ?? "");
		setNonKeywordCommentReplyText(s?.nonKeywordCommentReplyText ?? "");
		setNonKeywordReplyEnabled(true);
		setNonFollowerMessages([{ messageType: "TEXT", textContent: s?.nonFollowerMessageText ?? DEFAULT_NON_FOLLOWER_TEXT }]);
	};

	const loadTemplates = (targetPage: number) => {
		setLoading(true);
		api
			.listTemplates(targetPage)
			.then((res) => {
				setTemplates(res.content);
				setTotalPages(res.totalPages);
				setPageSize(res.size);
			})
			.catch((err) => setError(err instanceof Error ? err.message : "템플릿을 불러오지 못했습니다"))
			.finally(() => setLoading(false));
	};

	useEffect(() => {
		loadTemplates(page);
	}, [page]);

	useEffect(() => {
		api
			.getSettings()
			.then((s) => {
				setSettings(s);
				applyDefaultTexts(s);
			})
			.catch(() => setSettings(null));
	}, []);

	const ensurePostsLoaded = () => {
		if (posts.length === 0 && !postsLoading) {
			setPostsLoading(true);
			setPostsError(null);
			api
				.listPosts()
				.then(setPosts)
				.catch((err) => setPostsError(err instanceof Error ? err.message : "게시물 목록을 불러오지 못했습니다"))
				.finally(() => setPostsLoading(false));
		}
	};

	const closeForm = () => {
		setShowForm(false);
		setEditingId(null);
	};

	const openCreateForm = () => {
		setEditingId(null);
		resetForm();
		setShowForm(true);
		ensurePostsLoaded();
	};

	const applyDetail = (detail: TemplateDetailResponse) => {
		setName(detail.name);
		setPostId(detail.postId);
		setUseSchedule(detail.dispatchTime != null);
		setDispatchTime(detail.dispatchTime ? detail.dispatchTime.slice(0, 5) : "");
		setKeywordsText(detail.keywords.join(", "));
		setUseDmKeyword(detail.dmKeyword != null);
		setDmKeyword(detail.dmKeyword ?? "");
		setCommentReplyText(detail.commentReplyText ?? "");
		setNonKeywordCommentReplyText(detail.nonKeywordCommentReplyText ?? "");
		setNonKeywordReplyEnabled(detail.nonKeywordReplyEnabled);
		setFollowerMessages(detail.followerMessages.length > 0 ? detail.followerMessages : [emptyMessage()]);
		setNonFollowerMessages(
			detail.nonFollowerMessages.length > 0
				? detail.nonFollowerMessages
				: [{ messageType: "TEXT", textContent: settings?.nonFollowerMessageText ?? DEFAULT_NON_FOLLOWER_TEXT }],
		);
	};

	const openEditForm = async (id: number) => {
		setEditingId(id);
		setFormError(null);
		setShowForm(true);
		ensurePostsLoaded();
		try {
			const detail = await api.getTemplate(id);
			applyDetail(detail);
		} catch (err) {
			setFormError(err instanceof Error ? err.message : "템플릿 정보를 불러오지 못했습니다");
		}
	};

	const updateMessage = (
		list: MessageInput[],
		setList: (v: MessageInput[]) => void,
		index: number,
		patch: Partial<MessageInput>,
	) => {
		setList(list.map((m, i) => (i === index ? { ...m, ...patch } : m)));
	};

	const addMessage = (list: MessageInput[], setList: (v: MessageInput[]) => void) => {
		if (list.length >= maxMessages) return;
		setList([...list, emptyMessage()]);
	};

	const removeMessage = (list: MessageInput[], setList: (v: MessageInput[]) => void, index: number) => {
		setList(list.filter((_, i) => i !== index));
	};

	const updateButton = (
		list: MessageInput[],
		setList: (v: MessageInput[]) => void,
		messageIndex: number,
		buttonIndex: number,
		patch: Partial<ButtonInput>,
	) => {
		const buttons = (list[messageIndex].buttons ?? []).map((b, i) => (i === buttonIndex ? { ...b, ...patch } : b));
		updateMessage(list, setList, messageIndex, { buttons });
	};

	const addButton = (list: MessageInput[], setList: (v: MessageInput[]) => void, messageIndex: number) => {
		const buttons = list[messageIndex].buttons ?? [];
		if (buttons.length >= MAX_BUTTONS) return;
		updateMessage(list, setList, messageIndex, { buttons: [...buttons, { title: "", url: "" }] });
	};

	const removeButton = (
		list: MessageInput[],
		setList: (v: MessageInput[]) => void,
		messageIndex: number,
		buttonIndex: number,
	) => {
		const buttons = (list[messageIndex].buttons ?? []).filter((_, i) => i !== buttonIndex);
		updateMessage(list, setList, messageIndex, { buttons });
	};

	const handleImageSelect = async (
		list: MessageInput[],
		setList: (v: MessageInput[]) => void,
		index: number,
		file: File,
		uploadKey: string,
	) => {
		setUploadingIndex(uploadKey);
		try {
			const { url } = await api.uploadMedia(file);
			updateMessage(list, setList, index, { imageUrl: url });
		} catch (err) {
			alert(err instanceof Error ? err.message : "이미지 업로드에 실패했습니다");
		} finally {
			setUploadingIndex(null);
		}
	};

	const resetForm = () => {
		setName("");
		setPostId(null);
		setUseSchedule(false);
		setDispatchTime("");
		setKeywordsText("");
		setUseDmKeyword(false);
		setDmKeyword("");
		setFollowerMessages([emptyMessage()]);
		applyDefaultTexts(settings);
	};

	const handleSubmit = async (e: FormEvent) => {
		e.preventDefault();
		setFormError(null);

		if (postId == null) {
			setFormError("게시물을 선택해주세요");
			return;
		}

		setSubmitting(true);
		const body: CreateTemplateRequest = {
			name,
			postId,
			dispatchTime: useSchedule && dispatchTime ? `${dispatchTime}:00` : null,
			keywords: keywordsText
				.split(",")
				.map((k) => k.trim())
				.filter(Boolean),
			dmKeyword: useDmKeyword ? dmKeyword || undefined : undefined,
			commentReplyText: commentReplyText || undefined,
			nonKeywordCommentReplyText: nonKeywordReplyEnabled ? nonKeywordCommentReplyText || undefined : undefined,
			nonKeywordReplyEnabled,
			followerMessages,
			nonFollowerMessages,
		};

		try {
			if (editingId != null) {
				await api.updateTemplate(editingId, body);
			} else {
				await api.createTemplate(body);
			}
			setShowForm(false);
			setEditingId(null);
			resetForm();
			if (page === 0) loadTemplates(0);
			else setPage(0);
		} catch (err) {
			setFormError(err instanceof Error ? err.message : (editingId != null ? "템플릿 수정에 실패했습니다" : "템플릿 생성에 실패했습니다"));
		} finally {
			setSubmitting(false);
		}
	};

	const handleDelete = async (id: number) => {
		if (!window.confirm("이 템플릿을 삭제하시겠습니까?")) return;
		try {
			await api.deleteTemplate(id);
			setShowForm(false);
			setEditingId(null);
			loadTemplates(page);
		} catch (err) {
			alert(err instanceof Error ? err.message : "삭제에 실패했습니다");
		}
	};

	const handleToggleActive = async (e: MouseEvent<HTMLInputElement>, t: TemplateResponse) => {
		e.preventDefault();
		e.stopPropagation();
		if (t.activeYn && !window.confirm("이 템플릿 사용을 중지하시겠습니까?")) return;
		try {
			const updated = await api.updateTemplateActiveYn(t.id, !t.activeYn);
			setTemplates((prev) => prev.map((item) => (item.id === updated.id ? updated : item)));
		} catch (err) {
			alert(err instanceof Error ? err.message : "사용 여부 변경에 실패했습니다");
		}
	};

	const renderMessageEditor = (label: string, list: MessageInput[], setList: (v: MessageInput[]) => void, keyPrefix: string) => (
		<fieldset className="message-fieldset">
			<legend>
				{label} (최대 {maxMessages}개)
			</legend>
			{list.map((message, index) => {
				const uploadKey = `${keyPrefix}-${index}`;
				return (
					<div key={index} className="message-editor">
						<select
							value={message.messageType}
							onChange={(e) => updateMessage(list, setList, index, { messageType: e.target.value as MessageType })}
						>
							<option value="TEXT">텍스트</option>
							<option value="IMAGE">이미지</option>
							<option value="BUTTON">버튼형</option>
						</select>
						{message.messageType === "TEXT" && (
							<textarea
								placeholder="메시지 내용"
								value={message.textContent ?? ""}
								onChange={(e) => updateMessage(list, setList, index, { textContent: e.target.value })}
							/>
						)}
						{message.messageType === "IMAGE" && (
							<div>
								<input
									type="file"
									accept="image/*"
									onChange={(e) => {
										const file = e.target.files?.[0];
										if (file) handleImageSelect(list, setList, index, file, uploadKey);
									}}
								/>
								{uploadingIndex === uploadKey && <p className="hint">업로드 중...</p>}
								{message.imageUrl && (
									<div className="image-preview">
										<img src={message.imageUrl} alt="첨부 이미지" />
									</div>
								)}
							</div>
						)}
						{message.messageType === "BUTTON" && (
							<div className="button-editor">
								<textarea
									placeholder="메시지 내용"
									value={message.textContent ?? ""}
									onChange={(e) => updateMessage(list, setList, index, { textContent: e.target.value })}
								/>
								{(message.buttons ?? []).map((button, buttonIndex) => (
									<div key={buttonIndex} className="button-input-row">
										<input
											type="text"
											placeholder="버튼 텍스트"
											value={button.title}
											onChange={(e) => updateButton(list, setList, index, buttonIndex, { title: e.target.value })}
										/>
										<input
											type="text"
											placeholder="이동할 URL"
											value={button.url}
											onChange={(e) => updateButton(list, setList, index, buttonIndex, { url: e.target.value })}
										/>
										<button type="button" onClick={() => removeButton(list, setList, index, buttonIndex)}>
											버튼 제거
										</button>
									</div>
								))}
								{(message.buttons ?? []).length < MAX_BUTTONS && (
									<button type="button" onClick={() => addButton(list, setList, index)}>
										+ 버튼 추가 ({(message.buttons ?? []).length}/{MAX_BUTTONS})
									</button>
								)}
							</div>
						)}
						{list.length > 1 && (
							<button type="button" onClick={() => removeMessage(list, setList, index)}>
								제거
							</button>
						)}
					</div>
				);
			})}
			{list.length < maxMessages && (
				<button type="button" onClick={() => addMessage(list, setList)}>
					+ 메시지 추가
				</button>
			)}
		</fieldset>
	);

	const selectedPost = posts.find((p) => p.id === postId);

	return (
		<div>
			<div className="page-header">
				<h2>{editingId != null ? "템플릿 수정" : "템플릿"}</h2>
				<button className={showForm ? "" : "primary-button"} onClick={() => (showForm ? closeForm() : openCreateForm())}>
					{showForm ? "취소" : "+ 템플릿 추가"}
				</button>
			</div>

			{showForm && (
				<form onSubmit={handleSubmit} className="template-form">
					{formError && <p className="error">{formError}</p>}
					<label>
						템플릿명
						<input value={name} onChange={(e) => setName(e.target.value)} required />
					</label>

					<div>
						<span className="field-label">게시물 선택</span>
						{postsLoading && <p className="hint">게시물 불러오는 중...</p>}
						{postsError && <p className="error">{postsError}</p>}
						{!postsLoading && !postsError && posts.length === 0 && <p className="hint">불러올 게시물이 없습니다.</p>}
						{posts.length > 0 && (
							<div className="post-picker">
								{posts.map((post) => (
									<button
										type="button"
										key={post.id}
										className={`post-card ${postId === post.id ? "selected" : ""}`}
										onClick={() => setPostId(post.id)}
									>
										{post.thumbnailUrl ? (
											<img src={post.thumbnailUrl} alt={post.caption ?? "게시물"} />
										) : (
											<div className="post-card-placeholder">이미지 없음</div>
										)}
										<span className="post-caption">{post.caption ? post.caption.slice(0, 30) : "(캡션 없음)"}</span>
									</button>
								))}
							</div>
						)}
						{postId != null && (
							<p className="hint">
								선택됨:{" "}
								{selectedPost
									? (selectedPost.caption?.slice(0, 40) ?? selectedPost.platformPostId)
									: `게시물 ID ${postId} (목록에 없지만 유지됩니다)`}
							</p>
						)}
					</div>

					<label className="checkbox-label">
						<input type="checkbox" checked={useSchedule} onChange={(e) => setUseSchedule(e.target.checked)} />
						예약 발송 사용 (체크 안 하면 즉시 발송)
					</label>
					{useSchedule && (
						<label>
							발송 예약 시각
							<input type="time" value={dispatchTime} onChange={(e) => setDispatchTime(e.target.value)} required={useSchedule} />
						</label>
					)}

					<label>
						키워드 (쉼표로 구분, 비우면 모든 댓글이 키워드 매칭으로 처리됨)
						<input value={keywordsText} onChange={(e) => setKeywordsText(e.target.value)} placeholder="이벤트, 참여" />
					</label>

					<div className="checkbox-group">
						<label className="checkbox-label">
							<input type="checkbox" checked={useDmKeyword} onChange={(e) => setUseDmKeyword(e.target.checked)} />
							DM으로 직접 문의 시 자동 응답 사용
						</label>
						<p className="hint">
							댓글에 버튼이 담긴 DM을 보냈는데도 사용자에게 메시지가 도착하지 않는 경우가 있습니다. 이 기능을 켜두면, 사용자가 DM
							대화창에 직접 특정 키워드를 입력했을 때도 같은 메시지가 발송됩니다.
						</p>
						{useDmKeyword && (
							<label>
								키워드
								<input
									value={dmKeyword}
									onChange={(e) => setDmKeyword(e.target.value)}
									placeholder="예: 이벤트"
									required={useDmKeyword}
								/>
							</label>
						)}
					</div>

					<label>
						키워드 댓글에 대한 응답
						<input
							value={commentReplyText}
							onChange={(e) => setCommentReplyText(e.target.value)}
							placeholder="메시지 보냈어요! DM이 안보이면 말씀주세요!"
						/>
					</label>
					<p className="hint">비워두면 기본 문구 "메시지 보냈어요! DM이 안보이면 말씀주세요!"가 사용됩니다.</p>

					<div className="checkbox-group">
						<span className="field-label">키워드가 아닌 댓글에 대한 응답</span>
						<label className="checkbox-label">
							<input
								type="checkbox"
								checked={!nonKeywordReplyEnabled}
								onChange={() => setNonKeywordReplyEnabled(false)}
							/>
							응답하지 않음
						</label>
						<label className="checkbox-label">
							<input type="checkbox" checked={nonKeywordReplyEnabled} onChange={() => setNonKeywordReplyEnabled(true)} />
							답글
						</label>
						{nonKeywordReplyEnabled && (
							<>
								<input
									value={nonKeywordCommentReplyText}
									onChange={(e) => setNonKeywordCommentReplyText(e.target.value)}
									placeholder="댓글 남겨주셔서 감사합니다!"
								/>
								<p className="hint">비워두면 기본 문구 "댓글 남겨주셔서 감사합니다!"가 사용됩니다.</p>
							</>
						)}
					</div>

					{renderMessageEditor("팔로워에게 보낼 메시지", followerMessages, setFollowerMessages, "follower")}
					{renderMessageEditor("논팔로워에게 보낼 메시지", nonFollowerMessages, setNonFollowerMessages, "nonfollower")}

					<div className="form-actions">
						<button
							type="submit"
							className={editingId != null ? "warning-button" : "primary-button"}
							disabled={submitting}
						>
							{submitting ? (editingId != null ? "수정 중..." : "생성 중...") : editingId != null ? "수정" : "템플릿 생성"}
						</button>
						{editingId != null && (
							<button type="button" className="danger-button" onClick={() => handleDelete(editingId)}>
								삭제
							</button>
						)}
					</div>
				</form>
			)}

			{!showForm && (
				<>
					{loading && <p>불러오는 중...</p>}
					{error && <p className="error">{error}</p>}

					<table className="data-table">
						<thead>
							<tr>
								<th>순번</th>
								<th>템플릿명</th>
								<th>키워드</th>
								<th className="hide-mobile">DM 키워드</th>
								<th className="hide-mobile">발송 시각</th>
								<th>사용여부</th>
							</tr>
						</thead>
						<tbody>
							{templates.map((t, index) => (
								<tr key={t.id} className="clickable-row" onClick={() => openEditForm(t.id)}>
									<td>{page * pageSize + index + 1}</td>
									<td>{t.name}</td>
									<td>{t.keywords.join(", ") || "-"}</td>
									<td className="hide-mobile">{t.dmKeyword ?? "-"}</td>
									<td className="hide-mobile">{t.dispatchTime ?? "즉시"}</td>
									<td>
										<label className="toggle-switch">
											<input type="checkbox" checked={t.activeYn} onClick={(e) => handleToggleActive(e, t)} readOnly />
											<span className="toggle-slider" />
										</label>
									</td>
								</tr>
							))}
						</tbody>
					</table>
					<Pagination page={page} totalPages={totalPages} onChange={setPage} />
				</>
			)}
		</div>
	);
}
