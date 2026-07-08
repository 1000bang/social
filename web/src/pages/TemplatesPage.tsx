import { useEffect, useState, type FormEvent } from "react";
import { api } from "../api/client";
import { Pagination } from "../components/Pagination";
import type { CreateTemplateRequest, MessageInput, MessageType, PostResponse, TemplateResponse } from "../api/types";

const MAX_MESSAGES = 3;
const DEFAULT_NON_FOLLOWER_TEXT = "팔로우가 확인되지 않았어요! 팔로우 후 다시 요청 부탁드립니다.";

function emptyMessage(): MessageInput {
	return { messageType: "TEXT", textContent: "" };
}

function defaultNonFollowerMessage(): MessageInput {
	return { messageType: "TEXT", textContent: DEFAULT_NON_FOLLOWER_TEXT };
}

export function TemplatesPage() {
	const [templates, setTemplates] = useState<TemplateResponse[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [showForm, setShowForm] = useState(false);
	const [page, setPage] = useState(0);
	const [totalPages, setTotalPages] = useState(0);

	const [posts, setPosts] = useState<PostResponse[]>([]);
	const [postsLoading, setPostsLoading] = useState(false);
	const [postsError, setPostsError] = useState<string | null>(null);

	const [name, setName] = useState("");
	const [postId, setPostId] = useState<number | null>(null);
	const [useSchedule, setUseSchedule] = useState(false);
	const [dispatchTime, setDispatchTime] = useState("");
	const [keywordsText, setKeywordsText] = useState("");
	const [useDmKeyword, setUseDmKeyword] = useState(false);
	const [dmKeyword, setDmKeyword] = useState("");
	const [commentReplyText, setCommentReplyText] = useState("");
	const [nonKeywordCommentReplyText, setNonKeywordCommentReplyText] = useState("");
	const [followerMessages, setFollowerMessages] = useState<MessageInput[]>([emptyMessage()]);
	const [nonFollowerMessages, setNonFollowerMessages] = useState<MessageInput[]>([defaultNonFollowerMessage()]);
	const [submitting, setSubmitting] = useState(false);
	const [formError, setFormError] = useState<string | null>(null);
	const [uploadingIndex, setUploadingIndex] = useState<string | null>(null);

	const loadTemplates = (targetPage: number) => {
		setLoading(true);
		api
			.listTemplates(targetPage)
			.then((res) => {
				setTemplates(res.content);
				setTotalPages(res.totalPages);
			})
			.catch((err) => setError(err instanceof Error ? err.message : "템플릿을 불러오지 못했습니다"))
			.finally(() => setLoading(false));
	};

	useEffect(() => {
		loadTemplates(page);
	}, [page]);

	const openForm = () => {
		setShowForm(true);
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

	const updateMessage = (
		list: MessageInput[],
		setList: (v: MessageInput[]) => void,
		index: number,
		patch: Partial<MessageInput>,
	) => {
		setList(list.map((m, i) => (i === index ? { ...m, ...patch } : m)));
	};

	const addMessage = (list: MessageInput[], setList: (v: MessageInput[]) => void) => {
		if (list.length >= MAX_MESSAGES) return;
		setList([...list, emptyMessage()]);
	};

	const removeMessage = (list: MessageInput[], setList: (v: MessageInput[]) => void, index: number) => {
		setList(list.filter((_, i) => i !== index));
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
		setCommentReplyText("");
		setNonKeywordCommentReplyText("");
		setFollowerMessages([emptyMessage()]);
		setNonFollowerMessages([defaultNonFollowerMessage()]);
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
			nonKeywordCommentReplyText: nonKeywordCommentReplyText || undefined,
			followerMessages,
			nonFollowerMessages,
		};

		try {
			await api.createTemplate(body);
			setShowForm(false);
			resetForm();
			if (page === 0) loadTemplates(0);
			else setPage(0);
		} catch (err) {
			setFormError(err instanceof Error ? err.message : "템플릿 생성에 실패했습니다");
		} finally {
			setSubmitting(false);
		}
	};

	const handleDelete = async (id: number) => {
		if (!window.confirm("이 템플릿을 삭제하시겠습니까?")) return;
		try {
			await api.deleteTemplate(id);
			loadTemplates(page);
		} catch (err) {
			alert(err instanceof Error ? err.message : "삭제에 실패했습니다");
		}
	};

	const renderMessageEditor = (label: string, list: MessageInput[], setList: (v: MessageInput[]) => void, keyPrefix: string) => (
		<fieldset className="message-fieldset">
			<legend>
				{label} (최대 {MAX_MESSAGES}개)
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
							<option value="CAROUSEL">캐러셀</option>
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
						{message.messageType === "CAROUSEL" && (
							<p className="hint">캐러셀 아이템 편집은 아직 지원하지 않습니다. 필요하면 API로 직접 생성해주세요.</p>
						)}
						{list.length > 1 && (
							<button type="button" onClick={() => removeMessage(list, setList, index)}>
								제거
							</button>
						)}
					</div>
				);
			})}
			{list.length < MAX_MESSAGES && (
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
				<h2>템플릿</h2>
				<button onClick={() => (showForm ? setShowForm(false) : openForm())}>{showForm ? "취소" : "+ 템플릿 추가"}</button>
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
						{selectedPost && <p className="hint">선택됨: {selectedPost.caption?.slice(0, 40) ?? selectedPost.platformPostId}</p>}
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

					<label>
						키워드가 아닌 댓글에 대한 응답
						<input
							value={nonKeywordCommentReplyText}
							onChange={(e) => setNonKeywordCommentReplyText(e.target.value)}
							placeholder="댓글 남겨주셔서 감사합니다!"
						/>
					</label>
					<p className="hint">비워두면 기본 문구 "댓글 남겨주셔서 감사합니다!"가 사용됩니다.</p>

					{renderMessageEditor("팔로워에게 보낼 메시지", followerMessages, setFollowerMessages, "follower")}
					{renderMessageEditor("논팔로워에게 보낼 메시지", nonFollowerMessages, setNonFollowerMessages, "nonfollower")}

					<button type="submit" disabled={submitting}>
						{submitting ? "생성 중..." : "템플릿 생성"}
					</button>
				</form>
			)}

			{!showForm && (
				<>
					{loading && <p>불러오는 중...</p>}
					{error && <p className="error">{error}</p>}

					<table className="data-table">
						<thead>
							<tr>
								<th>이름</th>
								<th>게시물 ID</th>
								<th>키워드</th>
								<th>DM 키워드</th>
								<th>발송 시각</th>
								<th />
							</tr>
						</thead>
						<tbody>
							{templates.map((t) => (
								<tr key={t.id}>
									<td>{t.name}</td>
									<td>{t.postId}</td>
									<td>{t.keywords.join(", ") || "-"}</td>
									<td>{t.dmKeyword ?? "-"}</td>
									<td>{t.dispatchTime ?? "즉시"}</td>
									<td>
										<button onClick={() => handleDelete(t.id)}>삭제</button>
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
