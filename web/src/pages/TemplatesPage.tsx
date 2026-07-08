import { useEffect, useState, type FormEvent } from "react";
import { api } from "../api/client";
import type { CreateTemplateRequest, MessageInput, MessageType, TemplateResponse } from "../api/types";

const MAX_MESSAGES = 3;

function emptyMessage(): MessageInput {
	return { messageType: "TEXT", textContent: "" };
}

export function TemplatesPage() {
	const [templates, setTemplates] = useState<TemplateResponse[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [showForm, setShowForm] = useState(false);

	const [name, setName] = useState("");
	const [postId, setPostId] = useState("");
	const [dispatchTime, setDispatchTime] = useState("");
	const [keywordsText, setKeywordsText] = useState("");
	const [dmKeyword, setDmKeyword] = useState("");
	const [commentReplyText, setCommentReplyText] = useState("");
	const [followerMessages, setFollowerMessages] = useState<MessageInput[]>([emptyMessage()]);
	const [nonFollowerMessages, setNonFollowerMessages] = useState<MessageInput[]>([emptyMessage()]);
	const [submitting, setSubmitting] = useState(false);
	const [formError, setFormError] = useState<string | null>(null);

	const loadTemplates = () => {
		setLoading(true);
		api
			.listTemplates()
			.then(setTemplates)
			.catch((err) => setError(err instanceof Error ? err.message : "템플릿을 불러오지 못했습니다"))
			.finally(() => setLoading(false));
	};

	useEffect(() => {
		loadTemplates();
	}, []);

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

	const resetForm = () => {
		setName("");
		setPostId("");
		setDispatchTime("");
		setKeywordsText("");
		setDmKeyword("");
		setCommentReplyText("");
		setFollowerMessages([emptyMessage()]);
		setNonFollowerMessages([emptyMessage()]);
	};

	const handleSubmit = async (e: FormEvent) => {
		e.preventDefault();
		setSubmitting(true);
		setFormError(null);

		const body: CreateTemplateRequest = {
			name,
			postId: Number(postId),
			dispatchTime: dispatchTime ? `${dispatchTime}:00` : null,
			keywords: keywordsText
				.split(",")
				.map((k) => k.trim())
				.filter(Boolean),
			dmKeyword: dmKeyword || undefined,
			commentReplyText: commentReplyText || undefined,
			followerMessages,
			nonFollowerMessages,
		};

		try {
			await api.createTemplate(body);
			setShowForm(false);
			resetForm();
			loadTemplates();
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
			loadTemplates();
		} catch (err) {
			alert(err instanceof Error ? err.message : "삭제에 실패했습니다");
		}
	};

	const renderMessageEditor = (label: string, list: MessageInput[], setList: (v: MessageInput[]) => void) => (
		<fieldset className="message-fieldset">
			<legend>
				{label} (최대 {MAX_MESSAGES}개)
			</legend>
			{list.map((message, index) => (
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
						<input
							placeholder="이미지 URL"
							value={message.imageUrl ?? ""}
							onChange={(e) => updateMessage(list, setList, index, { imageUrl: e.target.value })}
						/>
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
			))}
			{list.length < MAX_MESSAGES && (
				<button type="button" onClick={() => addMessage(list, setList)}>
					+ 메시지 추가
				</button>
			)}
		</fieldset>
	);

	return (
		<div>
			<div className="page-header">
				<h2>템플릿</h2>
				<button onClick={() => setShowForm((v) => !v)}>{showForm ? "취소" : "+ 템플릿 추가"}</button>
			</div>

			{showForm && (
				<form onSubmit={handleSubmit} className="template-form">
					{formError && <p className="error">{formError}</p>}
					<label>
						템플릿명
						<input value={name} onChange={(e) => setName(e.target.value)} required />
					</label>
					<label>
						게시물 ID
						<input value={postId} onChange={(e) => setPostId(e.target.value)} required inputMode="numeric" />
					</label>
					<p className="hint">게시물 목록 조회 기능은 아직 없어서, DB에 등록된 post.id를 직접 입력해야 합니다.</p>
					<label>
						발송 예약 시각 (비우면 즉시 발송)
						<input type="time" value={dispatchTime} onChange={(e) => setDispatchTime(e.target.value)} />
					</label>
					<label>
						키워드 (쉼표로 구분)
						<input value={keywordsText} onChange={(e) => setKeywordsText(e.target.value)} placeholder="이벤트, 참여" />
					</label>
					<label>
						DM 키워드 자동 답장 (선택)
						<input value={dmKeyword} onChange={(e) => setDmKeyword(e.target.value)} />
					</label>
					<label>
						댓글 대댓글 문구 (선택, 비우면 기본 문구)
						<input value={commentReplyText} onChange={(e) => setCommentReplyText(e.target.value)} />
					</label>

					{renderMessageEditor("팔로워에게 보낼 메시지", followerMessages, setFollowerMessages)}
					{renderMessageEditor("논팔로워에게 보낼 메시지", nonFollowerMessages, setNonFollowerMessages)}

					<button type="submit" disabled={submitting}>
						{submitting ? "생성 중..." : "템플릿 생성"}
					</button>
				</form>
			)}

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
		</div>
	);
}
