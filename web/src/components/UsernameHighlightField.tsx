import { useRef, type ChangeEvent, type ReactNode, type UIEvent } from "react";

// 발송 시 실제 수신자 사용자명으로 치환되는 토큰. 백엔드 MessagePayloadBuilder.USERNAME_PLACEHOLDER와 동일해야 한다.
export const USERNAME_PLACEHOLDER = "{{사용자이름}}";

interface UsernameHighlightFieldProps {
	value: string;
	onChange: (value: string) => void;
	placeholder?: string;
	multiline?: boolean;
}

function renderHighlighted(text: string): ReactNode[] {
	const segments = text.split(USERNAME_PLACEHOLDER);
	const nodes: ReactNode[] = [];
	segments.forEach((segment, i) => {
		if (segment) nodes.push(<span key={`t${i}`}>{segment}</span>);
		if (i < segments.length - 1) {
			nodes.push(
				<mark key={`m${i}`} className="username-token">
					{USERNAME_PLACEHOLDER}
				</mark>,
			);
		}
	});
	return nodes;
}

// 실제 input/textarea 위에 투명한 글자색으로 겹쳐두고, 뒤에 같은 텍스트를 색깔 입혀 보여주는 방식으로
// {{사용자이름}} 부분만 강조 색으로 표시한다. 입력/커서/선택 동작은 그대로 네이티브 요소가 처리한다.
export function UsernameHighlightField({ value, onChange, placeholder, multiline }: UsernameHighlightFieldProps) {
	const backdropRef = useRef<HTMLDivElement>(null);

	const syncScroll = (e: UIEvent<HTMLInputElement | HTMLTextAreaElement>) => {
		if (!backdropRef.current) return;
		backdropRef.current.scrollTop = e.currentTarget.scrollTop;
		backdropRef.current.scrollLeft = e.currentTarget.scrollLeft;
	};

	return (
		<div className="username-highlight-field">
			<div ref={backdropRef} className={`username-highlight-backdrop ${multiline ? "" : "single-line"}`} aria-hidden="true">
				{renderHighlighted(value)}
			</div>
			{multiline ? (
				<textarea
					className="username-highlight-input"
					value={value}
					placeholder={placeholder}
					onChange={(e: ChangeEvent<HTMLTextAreaElement>) => onChange(e.target.value)}
					onScroll={syncScroll}
				/>
			) : (
				<input
					className="username-highlight-input"
					value={value}
					placeholder={placeholder}
					onChange={(e: ChangeEvent<HTMLInputElement>) => onChange(e.target.value)}
					onScroll={syncScroll}
				/>
			)}
		</div>
	);
}
